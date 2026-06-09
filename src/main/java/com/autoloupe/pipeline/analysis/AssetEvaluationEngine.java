package com.autoloupe.pipeline.analysis;

import com.autoloupe.pipeline.analysis.domain.EvaluationReport;
import com.autoloupe.pipeline.analysis.domain.TriageMetric;
import com.autoloupe.pipeline.analysis.domain.ImageProcessingContext;
import com.autoloupe.pipeline.analysis.neural.NeuralSubjectLocator;
import com.autoloupe.pipeline.domain.AnalysisTransaction;
import com.autoloupe.pipeline.exception.EvaluatorProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AssetEvaluationEngine implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AssetEvaluationEngine.class);
    private final List<AssetEvaluator> evaluators;
    private final NeuralSubjectLocator locatorService;
    private final ExecutorService evaluationWorkerPool;
    private final Consumer<EvaluationReport> outputConsumer;

    public AssetEvaluationEngine(List<AssetEvaluator> evaluators, NeuralSubjectLocator locatorService, Consumer<EvaluationReport> consumer) {
        this.evaluators = List.copyOf(evaluators);
        this.locatorService = locatorService;
        // Spin up an unbounded execution plane for async analysis tasks
        this.evaluationWorkerPool = Executors.newVirtualThreadPerTaskExecutor();
        // Default to a logging line until the user wires up a real dashboard/database
        this.outputConsumer = consumer;
    }

    /**
     * Entry point for Stage 2 hand-off. Dispatches an isolated virtual thread
     * task to compile analysis reports without blocking ingestion.
     */
    public void submitForAnalysis(AnalysisTransaction transaction) {
        evaluationWorkerPool.submit(() -> {
            log.debug("Beginning evaluation pipeline execution for asset: {}", transaction.asset().id());

            ImageProcessingContext context = new ImageProcessingContext(
                    transaction.asset(),
                    transaction.previewFrame(),
                    locatorService.detectPrimarySubject(transaction.previewFrame()));

            List<TriageMetric> compiledMetrics = new ArrayList<>();
            for (AssetEvaluator evaluator : evaluators) {
                try {
                    TriageMetric metric = evaluator.evaluate(context);
                    compiledMetrics.add(metric);
                } catch (EvaluatorProcessingException e) {
                    log.error("Evaluator failure executing rule mapping on asset {}: {}", transaction.asset().id(), e.getMessage());
                    compiledMetrics.add(new TriageMetric(
                            evaluator.getClass().getSimpleName(),
                            "ERROR",
                            "Internal rule processing failure: " + e.getMessage()
                    ));
                } catch (RuntimeException e) {
                    log.error("Unexpected runtime error in evaluator {} for asset {}: {}", 
                            evaluator.getClass().getSimpleName(), transaction.asset().id(), e.getMessage(), e);
                    compiledMetrics.add(new TriageMetric(
                            evaluator.getClass().getSimpleName(),
                            "ERROR",
                            "Critical evaluator failure: " + e.getMessage()
                    ));
                }
            }

            EvaluationReport report = new EvaluationReport(
                    transaction.asset().id(),
                    transaction.asset().rawFilePath(),
                    Instant.now(),
                    List.copyOf(compiledMetrics)
            );

            outputConsumer.accept(report);
        });
    }

    @Override
    public void close() {
        log.info("[Auto Loupe] Winding down Stage 3 analysis workers...");
        evaluationWorkerPool.close(); // Use close() instead of shutdown() to wait for termination
    }
}
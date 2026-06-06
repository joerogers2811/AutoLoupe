package com.autoloupe.pipeline.analysis;

import com.autoloupe.pipeline.analysis.neural.NeuralSubjectLocator;
import com.autoloupe.pipeline.analysis.domain.EvaluationReport;
import com.autoloupe.pipeline.analysis.domain.TriageMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AssetEvaluationEngine implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AssetEvaluationEngine.class);
    private final List<AssetEvaluator> evaluators;
    private final ExecutorService evaluationWorkerPool;
    private Consumer<EvaluationReport> outputConsumer;

    public AssetEvaluationEngine(List<AssetEvaluator> evaluators) {
        this.evaluators = List.copyOf(evaluators);
        // Spin up an unbounded execution plane for async analysis tasks
        this.evaluationWorkerPool = Executors.newVirtualThreadPerTaskExecutor();
        // Default to a logging line until the user wires up a real dashboard/database
        this.outputConsumer = report -> log.info("[Analysis Complete] Asset ID: {} generated {} metrics.",
                report.assetId(), report.metrics().size());
    }

    public void registerOutputConsumer(Consumer<EvaluationReport> consumer) {
        this.outputConsumer = consumer;
    }

    /**
     * Entry point for Stage 2 hand-off. Dispatches an isolated virtual thread
     * task to compile analysis reports without blocking ingestion.
     */
    public void submitForAnalysis(ImageProcessingContext context) {
        evaluationWorkerPool.submit(() -> {
            log.debug("Beginning evaluation pipeline execution for asset: {}", context.asset().id());
            List<TriageMetric> compiledMetrics = new ArrayList<>();



            for (AssetEvaluator evaluator : evaluators) {
                try {
                    TriageMetric metric = evaluator.evaluate(context);
                    compiledMetrics.add(metric);
                } catch (Exception e) {
                    log.error("Evaluator failure executing rule mapping on asset {}: {}", context.asset().id(), e.getMessage(), e);
                    compiledMetrics.add(new TriageMetric(
                            evaluator.getClass().getSimpleName(),
                            "ERROR",
                            "Internal rule processing failure: " + e.getMessage()
                    ));
                }
            }

            EvaluationReport report = new EvaluationReport(
                    context.asset().id(),
                    context.asset().rawFilePath(),
                    Instant.now(),
                    List.copyOf(compiledMetrics)
            );

            outputConsumer.accept(report);
        });
    }

    @Override
    public void close() {
        log.info("[Auto Loupe] Winding down Stage 3 analysis workers...");
        evaluationWorkerPool.shutdown();
    }
}
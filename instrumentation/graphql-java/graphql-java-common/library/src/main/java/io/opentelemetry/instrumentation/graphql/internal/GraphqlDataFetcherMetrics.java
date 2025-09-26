/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.graphql.internal;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.logging.Level.FINE;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.OperationMetrics;
import io.opentelemetry.instrumentation.api.internal.OperationMetricsUtil;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * {@link OperationListener} which keeps track of GraphQL data fetcher metrics.
 */
final class GraphqlDataFetcherMetrics implements OperationListener {

  private static final double NANOS_PER_S = TimeUnit.SECONDS.toNanos(1);

  private static final ContextKey<State> GRAPHQL_DATAFETCHER_METRICS_STATE =
      ContextKey.named("graphql-datafetcher-metrics-state");

  private static final Logger logger = Logger.getLogger(GraphqlDataFetcherMetrics.class.getName());

  /**
   * Returns an {@link OperationMetrics} instance which can be used to enable recording of {@link
   * GraphqlDataFetcherMetrics}.
   *
   * @see InstrumenterBuilder#addOperationMetrics(OperationMetrics)
   */
  public static OperationMetrics get() {
    return OperationMetricsUtil.create("graphql data fetcher", GraphqlDataFetcherMetrics::new);
  }

  private final DoubleHistogram duration;

  private GraphqlDataFetcherMetrics(Meter meter) {

    DoubleHistogramBuilder builder = meter
        .histogramBuilder("graphql.datafetcher.duration")
        .setUnit("s")
        .setDescription("Duration of GraphQL data fetching.")
        .setExplicitBucketBoundariesAdvice(GraphqlMetricsAdvice.DURATION_SECONDS_BUCKETS);

    GraphqlMetricsAdvice.applyGraphqlDataFetcherDurationAdvice(builder);

    duration = builder.build();
  }

  @Override
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context.with(
        GRAPHQL_DATAFETCHER_METRICS_STATE,
        new AutoValue_GraphqlDataFetcherMetrics_State(startAttributes, startNanos));
  }

  @Override
  public void onEnd(Context context, Attributes endAttributes, long endNanos) {
    State state = context.get(GRAPHQL_DATAFETCHER_METRICS_STATE);
    if (state == null) {
      logger.log(
          FINE,
          "No state present when ending context {0}. Cannot record GraphQL operation metrics.",
          context);
      return;
    }

    Attributes attributes = state.startAttributes().toBuilder().putAll(endAttributes).build();

    duration.record((endNanos - state.startTimeNanos()) / NANOS_PER_S, attributes, context);
  }

  @AutoValue
  abstract static class State {

    abstract Attributes startAttributes();

    abstract long startTimeNanos();
  }
}

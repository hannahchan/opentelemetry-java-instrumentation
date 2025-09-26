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
 * {@link OperationListener} which keeps track of GraphQL operation metrics.
 */
final class GraphqlOperationMetrics implements OperationListener {

  private static final double NANOS_PER_S = TimeUnit.SECONDS.toNanos(1);

  private static final ContextKey<State> GRAPHQL_OPERATION_METRICS_STATE =
      ContextKey.named("graphql-operation-metrics-state");

  private static final Logger logger = Logger.getLogger(GraphqlOperationMetrics.class.getName());

  /**
   * Returns an {@link OperationMetrics} instance which can be used to enable recording of {@link
   * GraphqlOperationMetrics}.
   *
   * @see InstrumenterBuilder#addOperationMetrics(OperationMetrics)
   */
  public static OperationMetrics get() {
    return OperationMetricsUtil.create("graphql operation", GraphqlOperationMetrics::new);
  }

  private final DoubleHistogram duration;

  private GraphqlOperationMetrics(Meter meter) {

    DoubleHistogramBuilder builder = meter
        .histogramBuilder("graphql.operation.duration")
        .setUnit("s")
        .setDescription("Duration of GraphQL operations.")
        .setExplicitBucketBoundariesAdvice(GraphqlMetricsAdvice.DURATION_SECONDS_BUCKETS);

    GraphqlMetricsAdvice.applyGraphqlOperationDurationAdvice(builder);

    duration = builder.build();
  }

  @Override
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context.with(
        GRAPHQL_OPERATION_METRICS_STATE,
        new AutoValue_GraphqlOperationMetrics_State(startAttributes, startNanos));
  }

  @Override
  public void onEnd(Context context, Attributes endAttributes, long endNanos) {
    State state = context.get(GRAPHQL_OPERATION_METRICS_STATE);
    if (state == null) {
      logger.log(
          FINE,
          "No state present when ending context {0}. Cannot record GraphQL data fetching metrics.",
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

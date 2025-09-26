package io.opentelemetry.instrumentation.graphql.internal;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogramBuilder;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

final class GraphqlMetricsAdvice {

  static final List<Double> DURATION_SECONDS_BUCKETS = unmodifiableList(
      asList(0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0));

  static void applyGraphqlOperationDurationAdvice(DoubleHistogramBuilder builder) {

    if (!(builder instanceof ExtendedDoubleHistogramBuilder)) {
      return;
    }

    ((ExtendedDoubleHistogramBuilder) builder)
        .setAttributesAdvice(
            asList(
                AttributeKey.stringKey("key1"),
                AttributeKey.stringKey("key2"),
                AttributeKey.stringKey("key3"),
                AttributeKey.stringKey("key4")));
  }

  static void applyGraphqlDataFetcherDurationAdvice(DoubleHistogramBuilder builder) {

    if (!(builder instanceof ExtendedDoubleHistogramBuilder)) {
      return;
    }

    ((ExtendedDoubleHistogramBuilder) builder)
        .setAttributesAdvice(
            asList(
                AttributeKey.stringKey("key5"),
                AttributeKey.stringKey("key6"),
                AttributeKey.stringKey("key7"),
                AttributeKey.stringKey("key8")));
  }

  private GraphqlMetricsAdvice() {}
}

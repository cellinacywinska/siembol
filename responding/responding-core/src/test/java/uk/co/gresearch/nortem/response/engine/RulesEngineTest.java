package uk.co.gresearch.nortem.response.engine;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uk.co.gresearch.nortem.response.common.*;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.co.gresearch.nortem.response.common.ResponseEvaluationResult.FILTERED;
import static uk.co.gresearch.nortem.response.common.ResponseEvaluationResult.MATCH;
import static uk.co.gresearch.nortem.response.common.ResponseEvaluationResult.NO_MATCH;

public class RulesEngineTest {

    private ResponseAlert alert;
    Evaluable rule;
    Evaluable ruleNext;
    List<Evaluable> rules;
    RulesEngine.Builder builder;
    TestMetricFactory metricFactory;
    RespondingResult ruleResult;
    RespondingResultAttributes resultAttributes;
    RespondingResult ruleNextResult;
    RespondingResultAttributes resultNextAttributes;
    RulesEngine engine;

    @Before
    public void setUp() {
        alert = new ResponseAlert();
        alert.put("field1", "VALUE1");
        alert.put("field2", "VALUE2");

        resultAttributes = new RespondingResultAttributes();
        resultAttributes.setAlert(alert);
        ruleResult = new RespondingResult(RespondingResult.StatusCode.OK, resultAttributes);
        rule = Mockito.mock(Evaluable.class);
        when(rule.evaluate(alert)).thenReturn(ruleResult);

        resultNextAttributes = new RespondingResultAttributes();
        resultNextAttributes.setAlert(alert);
        ruleNextResult = new RespondingResult(RespondingResult.StatusCode.OK, resultNextAttributes);
        ruleNext = Mockito.mock(Evaluable.class);
        when(ruleNext.evaluate(alert)).thenReturn(ruleNextResult);
        rules = Arrays.asList(rule, ruleNext);

        metricFactory = new TestMetricFactory();
        builder = new RulesEngine.Builder()
                .metricFactory(metricFactory)
                .rules(rules);

        engine = builder.build();
    }

    @Test
    public void testFirstRuleMatch() {
        resultAttributes.setResult(MATCH);
        RespondingResult result = engine.evaluate(alert);
        Mockito.verify(rule, times(1)).evaluate(alert);
        Mockito.verify(ruleNext, times(0)).evaluate(alert);

        Assert.assertEquals(RespondingResult.StatusCode.OK, result.getStatusCode());
        Assert.assertNotNull(result.getAttributes());
        Assert.assertEquals(MATCH, result.getAttributes().getResult());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_PROCESSED_MESSAGES.getName()).getValue());
        Assert.assertEquals(0, metricFactory
                .getCounter(MetricNames.ENGINE_ERROR_MESSAGES.getName()).getValue());
        Assert.assertEquals(0, metricFactory
                .getCounter(MetricNames.ENGINE_FILTERED_MESSAGES.getName()).getValue());
    }

    @Test
    public void testSecondRuleMatch() {
        resultAttributes.setResult(NO_MATCH);
        resultNextAttributes.setResult(MATCH);
        RespondingResult result = engine.evaluate(alert);
        Mockito.verify(rule, times(1)).evaluate(alert);
        Mockito.verify(ruleNext, times(1)).evaluate(alert);

        Assert.assertEquals(RespondingResult.StatusCode.OK, result.getStatusCode());
        Assert.assertNotNull(result.getAttributes());
        Assert.assertEquals(MATCH, result.getAttributes().getResult());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_PROCESSED_MESSAGES.getName()).getValue());
        Assert.assertEquals(0, metricFactory
                .getCounter(MetricNames.ENGINE_ERROR_MESSAGES.getName()).getValue());
        Assert.assertEquals(0, metricFactory
                .getCounter(MetricNames.ENGINE_FILTERED_MESSAGES.getName()).getValue());
    }

    @Test
    public void testFirstRuleFiltered() {
        resultAttributes.setResult(FILTERED);
        RespondingResult result = engine.evaluate(alert);
        Mockito.verify(rule, times(1)).evaluate(alert);
        Mockito.verify(ruleNext, times(0)).evaluate(alert);

        Assert.assertEquals(RespondingResult.StatusCode.OK, result.getStatusCode());
        Assert.assertNotNull(result.getAttributes());
        Assert.assertEquals(FILTERED, result.getAttributes().getResult());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_PROCESSED_MESSAGES.getName()).getValue());
        Assert.assertEquals(0, metricFactory
                .getCounter(MetricNames.ENGINE_ERROR_MESSAGES.getName()).getValue());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_FILTERED_MESSAGES.getName()).getValue());
    }

    @Test
    public void testSecondRuleFiltered() {
        resultAttributes.setResult(NO_MATCH);
        resultNextAttributes.setResult(FILTERED);
        RespondingResult result = engine.evaluate(alert);
        Mockito.verify(rule, times(1)).evaluate(alert);
        Mockito.verify(ruleNext, times(1)).evaluate(alert);

        Assert.assertEquals(RespondingResult.StatusCode.OK, result.getStatusCode());
        Assert.assertNotNull(result.getAttributes());
        Assert.assertEquals(FILTERED, result.getAttributes().getResult());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_PROCESSED_MESSAGES.getName()).getValue());
        Assert.assertEquals(0, metricFactory
                .getCounter(MetricNames.ENGINE_ERROR_MESSAGES.getName()).getValue());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_FILTERED_MESSAGES.getName()).getValue());
    }

    @Test
    public void testFirstRuleErrorMatch() {
        when(rule.evaluate(alert)).thenReturn(RespondingResult.fromException(new IllegalStateException()));
        RespondingResult result = engine.evaluate(alert);
        Mockito.verify(rule, times(1)).evaluate(alert);
        Mockito.verify(ruleNext, times(0)).evaluate(alert);

        Assert.assertEquals(RespondingResult.StatusCode.ERROR, result.getStatusCode());
        Assert.assertNotNull(result.getAttributes());
        Assert.assertNotNull(result.getAttributes().getMessage());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_PROCESSED_MESSAGES.getName()).getValue());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_ERROR_MESSAGES.getName()).getValue());
        Assert.assertEquals(0, metricFactory
                .getCounter(MetricNames.ENGINE_FILTERED_MESSAGES.getName()).getValue());
    }

    @Test
    public void testSecondRuleErrorMatch() {
        resultAttributes.setResult(NO_MATCH);
        when(ruleNext.evaluate(alert)).thenReturn(RespondingResult.fromException(new IllegalStateException()));
        RespondingResult result = engine.evaluate(alert);
        Mockito.verify(rule, times(1)).evaluate(alert);
        Mockito.verify(ruleNext, times(1)).evaluate(alert);

        Assert.assertEquals(RespondingResult.StatusCode.ERROR, result.getStatusCode());
        Assert.assertNotNull(result.getAttributes());
        Assert.assertNotNull(result.getAttributes().getMessage());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_PROCESSED_MESSAGES.getName()).getValue());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_ERROR_MESSAGES.getName()).getValue());
        Assert.assertEquals(0, metricFactory
                .getCounter(MetricNames.ENGINE_FILTERED_MESSAGES.getName()).getValue());
    }

    @Test
    public void testNoRuleMatch() {
        resultAttributes.setResult(NO_MATCH);
        resultNextAttributes.setResult(NO_MATCH);
        RespondingResult result = engine.evaluate(alert);
        Mockito.verify(rule, times(1)).evaluate(alert);
        Mockito.verify(ruleNext, times(1)).evaluate(alert);

        Assert.assertEquals(RespondingResult.StatusCode.ERROR, result.getStatusCode());
        Assert.assertNotNull(result.getAttributes());
        Assert.assertNotNull(result.getAttributes().getMessage());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_PROCESSED_MESSAGES.getName()).getValue());
        Assert.assertEquals(1, metricFactory
                .getCounter(MetricNames.ENGINE_ERROR_MESSAGES.getName()).getValue());
        Assert.assertEquals(0, metricFactory
                .getCounter(MetricNames.ENGINE_FILTERED_MESSAGES.getName()).getValue());
    }
}

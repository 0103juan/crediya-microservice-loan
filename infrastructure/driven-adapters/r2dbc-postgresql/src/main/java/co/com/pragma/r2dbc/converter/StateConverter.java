package co.com.pragma.r2dbc.converter;

import co.com.pragma.model.state.State;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

public class StateConverter {

    @WritingConverter
    public static class StateToIntegerConverter implements Converter<State, Integer> {
        @Override
        public Integer convert(State source) {
            return source.getId();
        }
    }

    @ReadingConverter
    public static class IntegerToStateConverter implements Converter<Integer, State> {
        @Override
        public State convert(Integer source) {
            return State.of(source);
        }
    }
}
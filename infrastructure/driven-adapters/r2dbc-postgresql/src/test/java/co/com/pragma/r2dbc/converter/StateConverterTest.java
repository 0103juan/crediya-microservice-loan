package co.com.pragma.r2dbc.converter;

import co.com.pragma.model.state.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StateConverterTest {

    private final StateConverter.StateToIntegerConverter roleToIntegerConverter = new StateConverter.StateToIntegerConverter();
    private final StateConverter.IntegerToStateConverter integerToStateConverter = new StateConverter.IntegerToStateConverter();

    @Test
    void shouldConvertStateToInteger() {
        State role = State.REVIEW_PENDING;

        Integer roleId = roleToIntegerConverter.convert(role);

        assertEquals(1, roleId);
    }

    @Test
    void shouldConvertIntegerToState() {

        Integer roleId = 2;


        State role = integerToStateConverter.convert(roleId);


        assertEquals(State.APPROVED, role);
    }
}
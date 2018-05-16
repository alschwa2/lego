package java;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.containsString;

public class TestClass{

    @Test
    public void testMethod() {

        assertThat("a", containsString("Hello"));
    }

}

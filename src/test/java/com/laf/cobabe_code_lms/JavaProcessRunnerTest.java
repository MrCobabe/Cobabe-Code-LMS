
package com.laf.cobabe_code_lms;

import com.laf.cobabe_code_lms.services.JavaProcessRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//import static org.junit.jupiter.api.Assertions.AssertTrue;

class JavaProcessRunnerTest {

    private final JavaProcessRunner runner =
            new JavaProcessRunner();

    @Test
    void helloWorldRunsSuccessfully() {

        String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello, Dragons!");
                    }
                }
                """;

        var result = runner.execute(code, "");

        System.out.println("Success: " + result.success());
        System.out.println("Output: " + result.output());
        System.out.println("Error: " + result.error());

        assertTrue(result.success(), result.error());
        assertTrue(result.output().contains("Hello, Dragons!"));
    }

    @Test
    void compilerErrorsAreReturned() {

        String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello")
                    }
                }
                """;

        var result = runner.execute(code, "");

        assertFalse(result.success());
        assertFalse(result.error().isBlank());
    }

    @Test
    void scannerInputWorks() {

        String code = """
                import java.util.Scanner;
                
                public class Main {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        String name = scanner.nextLine();
                        System.out.println("Hello, " + name + "!");
                    }
                }
                """;

        var result = runner.execute(code, "Dragon\n");

        assertTrue(result.success(), result.error());
        assertTrue(result.output().contains("Hello, Dragon!"));
    }
}

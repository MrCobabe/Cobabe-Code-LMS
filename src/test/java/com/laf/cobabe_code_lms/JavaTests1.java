
package com.laf.cobabe_code_lms;

import com.laf.cobabe_code_lms.services.JavaProcessRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JavaTests1 {

    private final JavaProcessRunner runner =
            new JavaProcessRunner();

    @Test
    public void helloWorldRunsSuccessfully() {
        String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello, Dragons!");
                    }
                }
                """;

        var result = runner.execute(code, "");

        assertTrue(result.success(), result.error());
        assertTrue(result.output().contains("Hello, Dragons!"));
    }

    @Test
    public void compilerErrorsAreReturned() {
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
    public void scannerInputWorks() {
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

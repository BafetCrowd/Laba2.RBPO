package рбпо.гришин.лаба2.контроллер;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class ОбработкаОшибок {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> ошибкаОперации(ResponseStatusException исключение) {
        String сообщение = исключение.getReason();
        if (сообщение == null) {
            сообщение = "Не удалось выполнить операцию.";
        }
        return ResponseEntity.status(исключение.getStatusCode()).body(Map.of("ошибка", сообщение));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> неверныеДанные() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("ошибка", "Проверьте JSON, названия полей и типы значений."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> неверныйНомер() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("ошибка", "Номер в адресе должен быть целым числом."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> неверныйМетод() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("ошибка", "Этот HTTP-метод не поддерживается по указанному адресу."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> неверныйАдрес() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("ошибка", "Указанный адрес не найден."));
    }
}

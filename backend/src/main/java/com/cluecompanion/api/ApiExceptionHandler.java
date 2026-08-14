package com.cluecompanion.api;
import com.cluecompanion.game.GameException; import java.util.Map; import org.springframework.http.HttpStatus; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(GameException.class) @ResponseStatus(HttpStatus.CONFLICT) Map<String,String> game(GameException e){return Map.of("error",e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,String> invalid(){return Map.of("error","Request is invalid");}
}

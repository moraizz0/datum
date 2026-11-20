package br.com.datum.controller;

import br.com.datum.dto.UploadDtos.EnvioValidadasRequest;
import br.com.datum.service.UploadService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Recebe as linhas que o front validou (Pinia). Exige login (qualquer perfil aprovado). */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService service;

    public UploadController(UploadService service) { this.service = service; }

    @PostMapping("/validadas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> receber(@RequestBody EnvioValidadasRequest req) {
        return service.receberValidadas(req);
    }
}

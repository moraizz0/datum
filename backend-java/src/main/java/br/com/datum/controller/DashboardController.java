package br.com.datum.controller;

import br.com.datum.service.DashboardService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/dashboard           -> último lote processado + JSON das contas
 * GET /api/dashboard?loteId=3  -> um lote específico
 * GET /api/dashboard/lotes     -> os últimos lotes (para escolher na tela)
 * Exige login (regra "anyRequest().authenticated()" do SecurityConfig).
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    public Map<String, Object> obter(@RequestParam(required = false) Long loteId) {
        return service.obter(loteId);
    }

    @GetMapping("/lotes")
    public List<Map<String, Object>> lotes() { return service.listarLotes(); }
}

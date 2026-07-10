package com.comanda.onboarding.seed;

import com.comanda.menu.domain.SelectionType;
import com.comanda.onboarding.Segment;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Demo cardápio per segment (design.md Decision 7): the dono edits real, already-persisted data
 * instead of starting from a blank menu (PRD Seção 5.1). Calibrated well under the Gratuito
 * limits (task 5.1: ≤ 30 produtos / ≤ 5 categorias) — every segment here is 2 categories / 4
 * produtos, so {@link com.comanda.onboarding.seed.OnboardingSeedService} never has to worry about
 * the seed itself tripping {@code PlanPolicyService}.
 */
public final class SeedCatalog {

    public record SeedAdditionalItem(String name, BigDecimal additionalPrice) {
    }

    public record SeedAdditionalGroup(
            String name, boolean required, SelectionType selectionType, int minSelections, Integer maxSelections,
            List<SeedAdditionalItem> items) {
    }

    public record SeedProduct(
            String name, String description, BigDecimal price, List<SeedAdditionalGroup> additionalGroups) {
    }

    public record SeedCategory(String name, List<SeedProduct> products) {
    }

    private static final Map<Segment, List<SeedCategory>> CATALOG = new EnumMap<>(Segment.class);

    static {
        CATALOG.put(Segment.MARMITARIA, List.of(
                new SeedCategory("Marmitas", List.of(
                        new SeedProduct(
                                "Marmita de Frango Grelhado",
                                "Frango grelhado, arroz, feijão e salada.",
                                new BigDecimal("18.00"),
                                List.of(new SeedAdditionalGroup(
                                        "Acompanhamentos", false, SelectionType.MULTIPLE, 0, null,
                                        List.of(
                                                new SeedAdditionalItem("Ovo frito", new BigDecimal("2.00")),
                                                new SeedAdditionalItem("Farofa", new BigDecimal("2.00")))))),
                        new SeedProduct(
                                "Marmita de Carne Moída",
                                "Carne moída refogada, arroz, feijão e salada.",
                                new BigDecimal("18.00"),
                                List.of()))),
                new SeedCategory("Bebidas", List.of(
                        new SeedProduct("Suco de Laranja 500ml", "Suco natural.", new BigDecimal("6.00"), List.of()),
                        new SeedProduct("Refrigerante Lata", "350ml, gelado.", new BigDecimal("5.00"), List.of())))));

        CATALOG.put(Segment.CONFEITARIA, List.of(
                new SeedCategory("Bolos", List.of(
                        new SeedProduct(
                                "Fatia de Bolo de Chocolate",
                                "Massa fofinha com cobertura de brigadeiro.",
                                new BigDecimal("9.00"), List.of()),
                        new SeedProduct(
                                "Fatia de Bolo Red Velvet",
                                "Com recheio de cream cheese.",
                                new BigDecimal("10.00"), List.of()))),
                new SeedCategory("Doces", List.of(
                        new SeedProduct(
                                "Brigadeiro Gourmet",
                                "Unidade, chocolate belga.",
                                new BigDecimal("3.50"), List.of()),
                        new SeedProduct("Beijinho", "Unidade.", new BigDecimal("3.00"), List.of())))));

        CATALOG.put(Segment.HAMBURGUERIA, List.of(
                new SeedCategory("Lanches", List.of(
                        new SeedProduct(
                                "X-Burger",
                                "Hambúrguer 150g, queijo e maionese da casa.",
                                new BigDecimal("22.00"),
                                List.of(new SeedAdditionalGroup(
                                        "Adicionais", false, SelectionType.MULTIPLE, 0, null,
                                        List.of(
                                                new SeedAdditionalItem("Bacon", new BigDecimal("4.00")),
                                                new SeedAdditionalItem("Ovo", new BigDecimal("2.50")),
                                                new SeedAdditionalItem("Queijo extra", new BigDecimal("3.00")))))),
                        new SeedProduct(
                                "X-Salada",
                                "Hambúrguer 150g, queijo, alface e tomate.",
                                new BigDecimal("20.00"), List.of()))),
                new SeedCategory("Bebidas", List.of(
                        new SeedProduct("Refrigerante Lata", "350ml, gelado.", new BigDecimal("5.00"), List.of()),
                        new SeedProduct("Suco Natural 500ml", "Sabor da casa.", new BigDecimal("6.00"), List.of())))));

        CATALOG.put(Segment.ACAIZERIA, List.of(
                new SeedCategory("Açaí", List.of(
                        new SeedProduct(
                                "Açaí 300ml",
                                "Açaí puro batido na hora.",
                                new BigDecimal("12.00"),
                                List.of(new SeedAdditionalGroup(
                                        "Acompanhamentos", false, SelectionType.MULTIPLE, 0, null,
                                        List.of(
                                                new SeedAdditionalItem("Granola", new BigDecimal("2.00")),
                                                new SeedAdditionalItem("Leite em pó", new BigDecimal("2.00")),
                                                new SeedAdditionalItem("Morango", new BigDecimal("3.00")))))),
                        new SeedProduct(
                                "Açaí 500ml",
                                "Açaí puro batido na hora.",
                                new BigDecimal("18.00"),
                                List.of(new SeedAdditionalGroup(
                                        "Acompanhamentos", false, SelectionType.MULTIPLE, 0, null,
                                        List.of(
                                                new SeedAdditionalItem("Granola", new BigDecimal("2.00")),
                                                new SeedAdditionalItem("Leite em pó", new BigDecimal("2.00")),
                                                new SeedAdditionalItem("Morango", new BigDecimal("3.00")))))))),
                new SeedCategory("Complementos", List.of(
                        new SeedProduct("Vitamina de Açaí 500ml", "Açaí batido com leite e banana.", new BigDecimal("14.00"), List.of())))));
    }

    private SeedCatalog() {
    }

    public static List<SeedCategory> forSegment(Segment segment) {
        return CATALOG.get(segment);
    }
}

package net.maximlvr.asmpthings.ai;

public enum AiNpcRelationState {
    DETESTE("deteste"),
    N_APPRECIE_PAS("n'apprecie pas"),
    N_AIME_PAS_TROP("n'aime pas trop"),
    SANS_PLUS("sans plus"),
    NEUTRE("neutre"),
    CONNAISSANCE("connaissance"),
    AIME_BIEN("aime bien"),
    APPRECIE_BEAUCOUP("apprécie beaucoup"),
    ADORE("adore"),
    SUPER_AMI("super ami");

    private final String label;

    AiNpcRelationState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public AiNpcRelationState up() {
        int nextOrdinal = Math.min(values().length - 1, ordinal() + 1);
        return values()[nextOrdinal];
    }

    public AiNpcRelationState down() {
        int previousOrdinal = Math.max(0, ordinal() - 1);
        return values()[previousOrdinal];
    }

    public static AiNpcRelationState fromName(String value) {
        if (value == null || value.isBlank()) {
            return NEUTRE;
        }

        String normalized = value.trim()
                .toLowerCase()
                .replace("_", " ")
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("'", "");

        return switch (normalized) {
            case "deteste" -> DETESTE;
            case "n apprecie pas", "napprecie pas" -> N_APPRECIE_PAS;
            case "n aime pas trop", "naime pas trop" -> N_AIME_PAS_TROP;
            case "sans plus" -> SANS_PLUS;
            case "neutre" -> NEUTRE;
            case "connaissance" -> CONNAISSANCE;
            case "aime bien" -> AIME_BIEN;
            case "apprecie beaucoup" -> APPRECIE_BEAUCOUP;
            case "adore" -> ADORE;
            case "super ami" -> SUPER_AMI;
            default -> NEUTRE;
        };
    }
}
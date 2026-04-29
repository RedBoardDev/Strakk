package com.strakk.shared.domain.model

enum class FeelingTag(val slug: String, val positive: Boolean) {
    EnergyStable("energy_stable", true),
    GoodEnergy("good_energy", true),
    Motivated("motivated", true),
    Disciplined("disciplined", true),
    GoodSleep("good_sleep", true),
    GoodRecovery("good_recovery", true),
    StrongTraining("strong_training", true),
    GoodMood("good_mood", true),
    Focused("focused", true),
    LightBody("light_body", true),
    GoodDigestion("good_digestion", true),

    LowEnergy("low_energy", false),
    Tired("tired", false),
    PoorSleep("poor_sleep", false),
    Stress("stress", false),
    LowMotivation("low_motivation", false),
    HeavyBody("heavy_body", false),
    Sore("sore", false),
    JointDiscomfort("joint_discomfort", false),
    DigestionDiscomfort("digestion_discomfort", false),
    Bloating("bloating", false),
    Hungry("hungry", false),
    Irritability("irritability", false),
    LowMood("low_mood", false),
    ;

    companion object {
        fun fromSlug(slug: String): FeelingTag? =
            entries.find { it.slug == slug }

        val positives: List<FeelingTag> get() = entries.filter { it.positive }
        val negatives: List<FeelingTag> get() = entries.filter { !it.positive }
    }
}

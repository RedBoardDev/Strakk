import shared

func asKotlinDouble(_ value: Double?) -> KotlinDouble? {
    value.map { KotlinDouble(double: $0) }
}

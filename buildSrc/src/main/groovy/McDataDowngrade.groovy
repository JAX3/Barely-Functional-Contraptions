import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Converts the mod's loot tables and advancements from Minecraft 26.3's data format (the one kept in src/main) to
 * 26.2's, as the 26.2 build copies them, so each file is written once. 26.3 changed:
 * <ul>
 *     <li>loot conditions: a single {@code "condition": {"type": ...}} (several in a {@code minecraft:all_of}) instead of
 *     a {@code "conditions": [{"condition": ...}]} list; {@code minecraft:match_block} ({@code blocks}, {@code state})
 *     was {@code minecraft:block_state_property} ({@code block}, {@code properties});</li>
 *     <li>loot functions: a single {@code "modifier": {"type": ...}} instead of {@code "functions": [{"function": ...}]};</li>
 *     <li>the {@code minecraft:recipe_unlocked} trigger: {@code "recipes"} instead of {@code "recipe"}.</li>
 * </ul>
 * Anything else passes through unchanged.
 */
class McDataDowngrade extends FilterReader {
    McDataDowngrade(Reader input) {
        super(new StringReader(convert(input.text)))
    }

    static String convert(String text) {
        JsonOutput.prettyPrint(JsonOutput.toJson(walk(new JsonSlurper().parseText(text))))
    }

    static Object walk(Object value) {
        if (value instanceof Map) {
            Map out = new LinkedHashMap()
            value.each { key, v ->
                if (key == 'condition' && v instanceof Map && v.containsKey('type')) {
                    out.conditions = conditions(v)
                } else if (key == 'modifier' && v instanceof Map && v.containsKey('type')) {
                    out.functions = [function(v)]
                } else if (key == 'recipes' && v instanceof String) {
                    out.recipe = v
                } else {
                    out[key] = walk(v)
                }
            }
            return out
        }
        if (value instanceof List) return value.collect { walk(it) }
        return value
    }

    /** A 26.3 condition as 26.2's list: an all_of becomes the list of its terms. */
    static List conditions(Map condition) {
        if (condition.type == 'minecraft:all_of') return condition.terms.collectMany { conditions(it as Map) }
        return [single(condition)]
    }

    static Map single(Map condition) {
        Map out = new LinkedHashMap()
        String type = condition.type
        if (type == 'minecraft:match_block') {
            out.condition = 'minecraft:block_state_property'
            if (condition.blocks) out.block = condition.blocks
            if (condition.state) out.properties = condition.state
            return out
        }
        out.condition = type
        condition.each { key, v -> if (key != 'type') out[key] = (key == 'terms' && v instanceof List) ? v.collect { single(it as Map) } : walk(v) }
        return out
    }

    static Map function(Map function) {
        Map out = new LinkedHashMap()
        out.function = function.type
        function.each { key, v -> if (key != 'type') out[key] = walk(v) }
        return out
    }
}

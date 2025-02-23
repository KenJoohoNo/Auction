package kenjoohono.auction.gui

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mojang.authlib.GameProfile
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.banner.Pattern
import org.bukkit.block.banner.PatternType
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File
import java.lang.reflect.Type
import java.time.Instant
import java.util.UUID

data class ChatComponent(
    val text: String? = "",
    val color: String? = "",
    val extra: List<ChatComponent>? = null
)

fun flattenChatComponent(comp: ChatComponent): String {
    val colorCode = if (!comp.color.isNullOrEmpty()) {
        try { ChatColor.valueOf(comp.color.uppercase()).toString() } catch (e: Exception) { "" }
    } else ""
    val base = comp.text ?: ""
    val extras = comp.extra?.joinToString("") { flattenChatComponent(it) } ?: ""
    return colorCode + base + extras
}

private fun parseOperation(opStr: String): AttributeModifier.Operation {
    return try {
        AttributeModifier.Operation::class.java.getField(opStr.uppercase()).get(null) as AttributeModifier.Operation
    } catch (ex: Exception) {
        AttributeModifier.Operation.ADD_NUMBER
    }
}

class AuctionMainGui {
    private val gson = Gson()

    fun openAuctionGui(player: Player, page: Int = 0) {
        val auctionItems = loadAuctionItems()
        val titleWithPage = "$TITLE (페이지: $page)"
        val inventory = Bukkit.createInventory(null, 54, titleWithPage)
        val startIndex = page * 45
        val endIndex = minOf(startIndex + 45, auctionItems.size)
        for (i in startIndex until endIndex) {
            inventory.setItem(i - startIndex, auctionItems[i])
        }
        val blackGlass = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply { setDisplayName(" ") }
        }
        for (slot in 45..53) {
            if (slot != 49) inventory.setItem(slot, blackGlass)
        }
        if (page > 0) {
            val prevArrow = ItemStack(Material.ARROW).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6이전 페이지"))
                }
            }
            inventory.setItem(48, prevArrow)
        }
        if (endIndex < auctionItems.size) {
            val nextArrow = ItemStack(Material.ARROW).apply {
                itemMeta = itemMeta?.apply {
                    setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a다음 페이지"))
                }
            }
            inventory.setItem(50, nextArrow)
        }
        val auctionStartItem = ItemStack(Material.ANVIL).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a경매 등록"))
            }
        }
        inventory.setItem(49, auctionStartItem)
        player.openInventory(inventory)
    }

    private fun loadAuctionItems(): List<ItemStack> {
        val itemsList = mutableListOf<Pair<Long, ItemStack>>()
        val auctionFolder = File("plugins/Auction")
        if (!auctionFolder.exists() || !auctionFolder.isDirectory) return emptyList()
        val parser = JSONParser()
        auctionFolder.listFiles()?.forEach { playerDir ->
            if (playerDir.isDirectory) {
                playerDir.listFiles { file -> file.extension == "json5" }?.forEach { jsonFile ->
                    try {
                        val jsonText = jsonFile.readText(Charsets.UTF_8)
                        val json = parser.parse(jsonText) as JSONObject
                        val timestampString = json["timestamp"] as String
                        val timestamp = parseTimestamp(timestampString)
                        val itemsArray = json["items"] as org.json.simple.JSONArray
                        val playerName = json["playerName"] as? String
                        val price = json["price"]?.toString() ?: ""
                        val fileName = jsonFile.nameWithoutExtension
                        for (i in 0 until itemsArray.size) {
                            val itemJson = itemsArray[i] as JSONObject
                            val materialName = itemJson["material"] as String
                            val material = Material.matchMaterial(materialName)
                            val amount = (itemJson["amount"] as Number).toInt()
                            if (material != null) {
                                var itemStack = ItemStack(material, amount)
                                if (itemJson.containsKey("meta")) {
                                    val metaValue = itemJson["meta"]
                                    val restoredMeta = when (metaValue) {
                                        is JSONObject -> restoreItemMetaManually(metaValue as Map<String, Any>, material)
                                        is String -> restoreItemMetaFromString(metaValue, material)
                                        else -> null
                                    }
                                    if (restoredMeta != null) {
                                        itemStack.itemMeta = restoredMeta
                                    }
                                }
                                if (playerName != null) {
                                    val meta = itemStack.itemMeta ?: Bukkit.getItemFactory().getItemMeta(material)
                                    val loreList = meta.lore?.toMutableList() ?: mutableListOf()
                                    loreList.add(ChatColor.translateAlternateColorCodes('&', "&7아이템 아이디 : &6$fileName"))
                                    loreList.add(ChatColor.translateAlternateColorCodes('&', "&7아이템 등록자 : &6$playerName"))
                                    loreList.add(ChatColor.translateAlternateColorCodes('&', "&7아이템 가격 : &6$price"))
                                    meta.lore = loreList
                                    itemStack.itemMeta = meta
                                }
                                itemsList.add(Pair(timestamp, itemStack))
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return itemsList.sortedBy { it.first }.map { it.second }
    }

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            Instant.parse(timestamp).toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }

    private fun restoreItemMetaManually(metaMap: Map<String, Any>, material: Material): ItemMeta? {
        var meta = Bukkit.getItemFactory().getItemMeta(material) ?: return null
        if (metaMap.containsKey("lore")) {
            val loreValue = metaMap["lore"]
            val rawLoreList: List<String>? = when (loreValue) {
                is List<*> -> loreValue.mapNotNull { it?.toString() }
                is String -> listOf(loreValue)
                else -> null
            }
            val processedLore = rawLoreList?.map { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    try {
                        val comp = gson.fromJson(trimmed, ChatComponent::class.java)
                        flattenChatComponent(comp)
                    } catch (ex: Exception) { line }
                } else trimmed
            }
            if (processedLore != null) meta.lore = processedLore
        }
        if (metaMap.containsKey("enchants")) {
            val enchantsValue = metaMap["enchants"]
            if (enchantsValue is Map<*, *>) {
                enchantsValue.forEach { (key, value) ->
                    val enchantKey = key?.toString() ?: return@forEach
                    val level = value?.toString()?.toIntOrNull() ?: 1
                    val enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey.removePrefix("minecraft:")))
                    if (enchantment != null) meta.addEnchant(enchantment, level, true)
                }
            }
        }
        if (metaMap.containsKey("stored-enchants") && material == Material.ENCHANTED_BOOK) {
            val storedEnchants = metaMap["stored-enchants"]
            if (storedEnchants is Map<*, *>) {
                val bookMeta = meta as? EnchantmentStorageMeta
                if (bookMeta != null) {
                    storedEnchants.forEach { (key, value) ->
                        val enchantKey = key?.toString() ?: return@forEach
                        val level = value?.toString()?.toIntOrNull() ?: 1
                        val enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey.removePrefix("minecraft:")))
                        if (enchantment != null) {
                            bookMeta.addStoredEnchant(enchantment, level, true)
                        }
                    }
                    meta = bookMeta
                }
            }
        }
        if (metaMap.containsKey("PublicBukkitValues")) {
            val pbvStr = metaMap["PublicBukkitValues"]?.toString() ?: ""
            try {
                val type: Type = object : TypeToken<Map<String, Any>>() {}.type
                val pbvMap: Map<String, Any> = gson.fromJson(pbvStr, type)
                val plugin = Bukkit.getPluginManager().getPlugin("Auction")!!
                val pdc = meta.persistentDataContainer
                pbvMap.forEach { (key, value) ->
                    val validKey = key.toString().replace(":", "_")
                    pdc.set(NamespacedKey(plugin, validKey), PersistentDataType.STRING, value.toString())
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        if (metaMap.containsKey("attribute-modifiers")) {
            val modifiersValue = metaMap["attribute-modifiers"]
            if (modifiersValue is Map<*, *>) {
                modifiersValue.forEach { (attrKeyRaw, modListRaw) ->
                    val attrKey = attrKeyRaw?.toString() ?: return@forEach
                    try {
                        val fieldName = attrKey.removePrefix("minecraft:").uppercase()
                        val attribute = try {
                            Attribute::class.java.getField(fieldName).get(null) as Attribute
                        } catch (e: Exception) {
                            null
                        }
                        if (attribute == null) return@forEach
                        if (modListRaw is List<*>) {
                            modListRaw.forEach { modObj ->
                                if (modObj is Map<*, *>) {
                                    val keyObj = modObj["key"] as? JSONObject
                                    val ns = keyObj?.get("namespace")?.toString() ?: "default"
                                    val keyStr = keyObj?.get("key")?.toString() ?: "default"
                                    val nsKey = NamespacedKey(ns, keyStr)
                                    val amount = modObj["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
                                    val opStr = modObj["operation"]?.toString()?.uppercase() ?: "ADD_NUMBER"
                                    val operation = parseOperation(opStr)
                                    val modifier = AttributeModifier(
                                        NamespacedKey.minecraft(fieldName.lowercase()),
                                        amount,
                                        operation
                                    )
                                    meta.addAttributeModifier(attribute, modifier)
                                }
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        }
        if (material == Material.PLAYER_HEAD && metaMap.containsKey("skull-owner") && meta is SkullMeta) {
            val skullOwnerData = metaMap["skull-owner"]
            if (skullOwnerData is Map<*, *>) {
                val profileData = skullOwnerData["profile"] as? Map<*, *>
                val playerName = profileData?.get("name")?.toString()
                if (playerName != null) meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName))
                val propertiesList = skullOwnerData["properties"]
                if (propertiesList is List<*>) {
                    for (prop in propertiesList) {
                        if (prop is Map<*, *>) {
                            if (prop["name"] == "textures") {
                                val textureValue = prop["value"]?.toString()
                                if (!textureValue.isNullOrEmpty() && playerName != null) {
                                    try {
                                        val uuid = try { UUID.fromString(profileData?.get("id")?.toString()) } catch (ex: Exception) { UUID.randomUUID() }
                                        val gameProfile = GameProfile(uuid, playerName)
                                        val propertyClass = Class.forName("com.mojang.authlib.properties.Property")
                                        val propertyConstructor = propertyClass.getConstructor(String::class.java, String::class.java)
                                        val textureProperty = propertyConstructor.newInstance("textures", textureValue)
                                        val propertiesField = GameProfile::class.java.getDeclaredField("properties")
                                        propertiesField.isAccessible = true
                                        val properties = propertiesField.get(gameProfile) as com.mojang.authlib.properties.PropertyMap
                                        properties.put("textures", textureProperty as com.mojang.authlib.properties.Property)
                                        val profileField = meta.javaClass.getDeclaredField("profile")
                                        profileField.isAccessible = true
                                        val constructor = profileField.type.getConstructor(GameProfile::class.java)
                                        val newProfileInstance = constructor.newInstance(gameProfile)
                                        profileField.set(meta, newProfileInstance)
                                    } catch (ex: Exception) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (metaMap.containsKey("meta-type") && metaMap["meta-type"].toString().uppercase() == "BANNER" && meta is BannerMeta) {
            if (metaMap.containsKey("item-name")) {
                val itemNameJson = metaMap["item-name"].toString()
                try {
                    val comp = gson.fromJson(itemNameJson, ChatComponent::class.java)
                    meta.setDisplayName(flattenChatComponent(comp))
                } catch (ex: Exception) {
                    meta.setDisplayName(itemNameJson)
                }
            }
            if (metaMap.containsKey("ItemFlags")) {
                val flagsValue = metaMap["ItemFlags"]
                if (flagsValue is List<*>) {
                    flagsValue.forEach { flag ->
                        try {
                            val flagEnum = org.bukkit.inventory.ItemFlag.valueOf(flag.toString())
                            meta.addItemFlags(flagEnum)
                        } catch (ex: Exception) {
                        }
                    }
                }
            }
            if (metaMap.containsKey("rarity")) {
                val rarity = metaMap["rarity"].toString()
                val plugin = Bukkit.getPluginManager().getPlugin("Auction")!!
                meta.persistentDataContainer.set(NamespacedKey(plugin, "rarity"), PersistentDataType.STRING, rarity)
            }
            if (metaMap.containsKey("patterns")) {
                val patternsValue = metaMap["patterns"]
                if (patternsValue is List<*>) {
                    val patterns = mutableListOf<Pattern>()
                    patternsValue.forEach { patternObj ->
                        if (patternObj is Map<*, *>) {
                            val colorStr = patternObj["color"]?.toString() ?: return@forEach
                            val patternObjInner = patternObj["pattern"] as? Map<*, *> ?: return@forEach
                            val patternName = patternObjInner["name"]?.toString() ?: return@forEach
                            val dyeColor = (DyeColor::class.java.enumConstants ?: emptyArray())
                                .firstOrNull { it.name.equals(colorStr, ignoreCase = true) } ?: return@forEach
                            val patternType = (PatternType::class.java.enumConstants ?: emptyArray())
                                .firstOrNull { it.toString().equals(patternName, ignoreCase = true) } ?: return@forEach
                            patterns.add(Pattern(dyeColor, patternType))
                        }
                    }
                    meta.patterns = patterns
                }
            }
        }
        return meta
    }

    private fun restoreItemMetaFromString(metaString: String, material: Material): ItemMeta? {
        return try {
            val type: Type = object : TypeToken<Map<String, Any>>() {}.type
            val metaMap: Map<String, Any> = gson.fromJson(metaString, type)
            restoreItemMetaManually(metaMap, material)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val TITLE = "경매"
    }
}
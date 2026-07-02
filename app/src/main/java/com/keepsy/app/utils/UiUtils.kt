package com.keepsy.app.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.keepsy.app.ui.theme.PrimaryPurple
import java.util.Locale

fun parseCategoryColor(hex: String?): Color {
    if (hex == null) return PrimaryPurple
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        PrimaryPurple
    }
}

@Composable
fun getCategoryIconVector(iconName: String?): ImageVector {
    return when (iconName) {
        "description" -> Icons.Default.Description
        "devices" -> Icons.Default.Devices
        "key" -> Icons.Default.VpnKey
        "medical_services" -> Icons.Default.MedicalServices
        "diamond" -> Icons.Default.Diamond
        "build" -> Icons.Default.Build
        "inventory_2" -> Icons.Default.Inventory2
        "home" -> Icons.Default.Home
        "more_horiz" -> Icons.Default.MoreHoriz
        else -> Icons.Default.Category
    }
}

@Composable
fun getSmartItemIconVector(itemName: String, categoryIconName: String?): ImageVector {
    val cleanName = itemName.lowercase(Locale.ROOT).trim()
    return when {
        cleanName.contains("headphone") || cleanName.contains("earphone") || cleanName.contains("headset") || cleanName.contains("earbud") -> Icons.Default.Headphones
        cleanName.contains("icecream") || cleanName.contains("ice cream") || cleanName.contains("gelato") || cleanName.contains("popsicle") -> Icons.Default.Icecream
        cleanName.contains("desk") || cleanName.contains("table") || cleanName.contains("counter") || cleanName.contains("workstation") -> Icons.Default.Desk
        cleanName.contains("computer") || cleanName.contains("laptop") || cleanName.contains("macbook") || cleanName.contains("chromebook") || cleanName.contains("desktop") || cleanName.contains("pc") -> Icons.Default.Laptop
        cleanName.contains("keyboard") || cleanName.contains("mouse") || cleanName.contains("trackpad") -> Icons.Default.Keyboard
        cleanName.contains("monitor") || cleanName.contains("display") || cleanName.contains("screen") -> Icons.Default.Tv
        cleanName.contains("tablet") || cleanName.contains("ipad") -> Icons.Default.TabletAndroid
        cleanName.contains("phone") || cleanName.contains("mobile") || cleanName.contains("iphone") || cleanName.contains("android") || cleanName.contains("smartphone") -> Icons.Default.PhoneAndroid
        cleanName.contains("console") || cleanName.contains("gamepad") || cleanName.contains("controller") || cleanName.contains("playstation") || cleanName.contains("xbox") || cleanName.contains("nintendo") || cleanName.contains("switch") -> Icons.Default.Gamepad
        cleanName.contains("speaker") || cleanName.contains("speakers") || cleanName.contains("audio") || cleanName.contains("sound") || cleanName.contains("subwoofer") -> Icons.Default.Speaker
        cleanName.contains("camera") || cleanName.contains("photo") || cleanName.contains("lens") || cleanName.contains("camcorder") || cleanName.contains("gopro") -> Icons.Default.CameraAlt
        cleanName.contains("charger") || cleanName.contains("cable") || cleanName.contains("wire") || cleanName.contains("adapter") || cleanName.contains("battery") || cleanName.contains("powerbank") || cleanName.contains("power bank") -> Icons.Default.BatteryChargingFull
        cleanName.contains("watch") || cleanName.contains("smartwatch") || cleanName.contains("clock") || cleanName.contains("timer") || cleanName.contains("stopwatch") -> Icons.Default.Watch
        cleanName.contains("tv") || cleanName.contains("television") || cleanName.contains("streaming") -> Icons.Default.Tv
        cleanName.contains("wifi") || cleanName.contains("router") || cleanName.contains("modem") || cleanName.contains("internet") -> Icons.Default.Wifi
        cleanName.contains("bluetooth") || cleanName.contains("wireless") -> Icons.Default.Bluetooth
        cleanName.contains("mic") || cleanName.contains("microphone") -> Icons.Default.Mic
        cleanName.contains("shirt") || cleanName.contains("tshirt") || cleanName.contains("pants") || cleanName.contains("jeans") || cleanName.contains("jacket") || cleanName.contains("coat") || cleanName.contains("suit") || cleanName.contains("skirt") || cleanName.contains("dress") || cleanName.contains("apparel") || cleanName.contains("wear") -> Icons.Default.Checkroom
        cleanName.contains("shoe") || cleanName.contains("shoes") || cleanName.contains("sneaker") || cleanName.contains("sneakers") || cleanName.contains("boot") || cleanName.contains("boots") || cleanName.contains("sandal") || cleanName.contains("sandals") || cleanName.contains("sock") || cleanName.contains("socks") || cleanName.contains("footwear") -> Icons.Default.DirectionsWalk
        cleanName.contains("glasses") || cleanName.contains("sunglasses") || cleanName.contains("spectacles") || cleanName.contains("goggles") || cleanName.contains("specs") -> Icons.Default.Visibility
        cleanName.contains("hat") || cleanName.contains("cap") || cleanName.contains("beanie") || cleanName.contains("helmet") || cleanName.contains("headwear") -> Icons.Default.Face
        cleanName.contains("bag") || cleanName.contains("backpack") || cleanName.contains("handbag") || cleanName.contains("purse") || cleanName.contains("tote") || cleanName.contains("wallet") || cleanName.contains("suitcase") || cleanName.contains("luggage") || cleanName.contains("briefcase") -> Icons.Default.Work
        cleanName.contains("diamond") || cleanName.contains("ring") || cleanName.contains("necklace") || cleanName.contains("jewelry") || cleanName.contains("earrings") || cleanName.contains("bracelet") || cleanName.contains("gem") || cleanName.contains("gold") || cleanName.contains("silver") || cleanName.contains("jewel") -> Icons.Default.Diamond
        cleanName.contains("chair") || cleanName.contains("sofa") || cleanName.contains("couch") || cleanName.contains("stool") || cleanName.contains("seat") || cleanName.contains("bench") || cleanName.contains("armchair") -> Icons.Default.Chair
        cleanName.contains("bed") || cleanName.contains("mattress") || cleanName.contains("pillow") || cleanName.contains("blanket") || cleanName.contains("sheet") || cleanName.contains("bedding") || cleanName.contains("comforter") -> Icons.Default.Bed
        cleanName.contains("lamp") || cleanName.contains("light") || cleanName.contains("bulb") || cleanName.contains("torch") || cleanName.contains("lantern") || cleanName.contains("flashlight") -> Icons.Default.Lightbulb
        cleanName.contains("refrigerator") || cleanName.contains("fridge") || cleanName.contains("microwave") || cleanName.contains("oven") || cleanName.contains("stove") || cleanName.contains("toaster") || cleanName.contains("blender") || cleanName.contains("kitchenware") || cleanName.contains("pot") || cleanName.contains("pan") -> Icons.Default.Kitchen
        cleanName.contains("door") || cleanName.contains("window") || cleanName.contains("gate") -> Icons.Default.DoorFront
        cleanName.contains("bathtub") || cleanName.contains("shower") || cleanName.contains("bath") || cleanName.contains("bathroom") || cleanName.contains("toilet") -> Icons.Default.Bathtub
        cleanName.contains("coffee") || cleanName.contains("tea") || cleanName.contains("mug") || cleanName.contains("cup") || cleanName.contains("espresso") || cleanName.contains("latte") || cleanName.contains("cappuccino") || cleanName.contains("cafe") || cleanName.contains("starbucks") -> Icons.Default.LocalCafe
        cleanName.contains("water") || cleanName.contains("drink") || cleanName.contains("beverage") || cleanName.contains("juice") || cleanName.contains("bottle") || cleanName.contains("soda") || cleanName.contains("can") || cleanName.contains("flask") || cleanName.contains("thermos") -> Icons.Default.LocalDrink
        cleanName.contains("food") || cleanName.contains("snack") || cleanName.contains("cookie") || cleanName.contains("cookies") || cleanName.contains("biscuit") || cleanName.contains("sandwich") || cleanName.contains("lunchbox") || cleanName.contains("plate") || cleanName.contains("meal") || cleanName.contains("dinner") || cleanName.contains("lunch") -> Icons.Default.Restaurant
        cleanName.contains("cake") || cleanName.contains("cupcake") || cleanName.contains("pastry") || cleanName.contains("pie") || cleanName.contains("muffin") || cleanName.contains("bakery") || cleanName.contains("dessert") -> Icons.Default.Cake
        cleanName.contains("pizza") -> Icons.Default.LocalPizza
        cleanName.contains("egg") || cleanName.contains("eggs") || cleanName.contains("breakfast") -> Icons.Default.Egg
        cleanName.contains("car") || cleanName.contains("vehicle") || cleanName.contains("automobile") || cleanName.contains("suv") || cleanName.contains("truck") || cleanName.contains("taxi") || cleanName.contains("cab") -> Icons.Default.DirectionsCar
        cleanName.contains("bike") || cleanName.contains("bicycle") || cleanName.contains("scooter") || cleanName.contains("motorcycle") || cleanName.contains("cycle") -> Icons.Default.DirectionsBike
        cleanName.contains("bus") -> Icons.Default.DirectionsBus
        cleanName.contains("train") || cleanName.contains("metro") || cleanName.contains("subway") || cleanName.contains("rail") -> Icons.Default.DirectionsTransit
        cleanName.contains("boat") || cleanName.contains("ship") || cleanName.contains("ferry") || cleanName.contains("yacht") -> Icons.Default.DirectionsBoat
        cleanName.contains("flight") || cleanName.contains("plane") || cleanName.contains("airplane") || cleanName.contains("ticket") || cleanName.contains("boarding") || cleanName.contains("passport") || cleanName.contains("visa") -> Icons.Default.Flight
        cleanName.contains("map") || cleanName.contains("gps") || cleanName.contains("navigation") || cleanName.contains("compass") || cleanName.contains("route") -> Icons.Default.Map
        cleanName.contains("hotel") || cleanName.contains("motel") || cleanName.contains("inn") || cleanName.contains("resort") -> Icons.Default.Hotel
        cleanName.contains("gas") || cleanName.contains("fuel") || cleanName.contains("petrol") || cleanName.contains("station") -> Icons.Default.LocalGasStation
        cleanName.contains("ball") || cleanName.contains("soccer") || cleanName.contains("football") || cleanName.contains("basketball") || cleanName.contains("tennis") || cleanName.contains("sports") || cleanName.contains("gym") || cleanName.contains("dumbbell") || cleanName.contains("dumbbells") || cleanName.contains("fitness") || cleanName.contains("workout") || cleanName.contains("exercise") -> Icons.Default.SportsBasketball
        cleanName.contains("tent") || cleanName.contains("camp") || cleanName.contains("camping") || cleanName.contains("forest") || cleanName.contains("hiking") || cleanName.contains("backpacking") || cleanName.contains("outdoor") || cleanName.contains("mountain") -> Icons.Default.Terrain
        cleanName.contains("trophy") || cleanName.contains("award") || cleanName.contains("prize") || cleanName.contains("winner") || cleanName.contains("medal") || cleanName.contains("championship") -> Icons.Default.EmojiEvents
        cleanName.contains("book") || cleanName.contains("notebook") || cleanName.contains("novel") || cleanName.contains("magazine") || cleanName.contains("journal") || cleanName.contains("diary") || cleanName.contains("textbook") || cleanName.contains("catalog") || cleanName.contains("dictionary") -> Icons.Default.Book
        cleanName.contains("pen") || cleanName.contains("pencil") || cleanName.contains("marker") || cleanName.contains("highlighter") || cleanName.contains("crayon") || cleanName.contains("stationery") || cleanName.contains("ink") || cleanName.contains("quill") || cleanName.contains("sharpener") -> Icons.Default.Create
        cleanName.contains("paint") || cleanName.contains("brush") || cleanName.contains("canvas") || cleanName.contains("sketchbook") || cleanName.contains("easel") || cleanName.contains("palette") || cleanName.contains("acrylic") || cleanName.contains("watercolor") -> Icons.Default.Brush
        cleanName.contains("music") || cleanName.contains("song") || cleanName.contains("guitar") || cleanName.contains("piano") || cleanName.contains("violin") || cleanName.contains("flute") || cleanName.contains("drums") || cleanName.contains("instrument") || cleanName.contains("melody") || cleanName.contains("track") -> Icons.Default.MusicNote
        cleanName.contains("movie") || cleanName.contains("film") || cleanName.contains("cinema") || cleanName.contains("show") || cleanName.contains("theater") || cleanName.contains("video") || cleanName.contains("dvd") || cleanName.contains("bluray") -> Icons.Default.Movie
        cleanName.contains("tool") || cleanName.contains("hammer") || cleanName.contains("screwdriver") || cleanName.contains("wrench") || cleanName.contains("pliers") || cleanName.contains("saw") || cleanName.contains("drill") || cleanName.contains("hardware") || cleanName.contains("screw") || cleanName.contains("bolt") || cleanName.contains("nail") || cleanName.contains("spanner") || cleanName.contains("toolkit") -> Icons.Default.Build
        cleanName.contains("scissors") || cleanName.contains("scissor") || cleanName.contains("clipper") || cleanName.contains("cut") || cleanName.contains("shear") || cleanName.contains("cutter") || cleanName.contains("blade") || cleanName.contains("knife") || cleanName.contains("pocketknife") -> Icons.Default.ContentCut
        cleanName.contains("key") || cleanName.contains("keys") || cleanName.contains("lock") || cleanName.contains("keychain") || cleanName.contains("padlock") || cleanName.contains("unlocked") || cleanName.contains("latch") || cleanName.contains("deadbolt") -> Icons.Default.VpnKey
        cleanName.contains("umbrella") || cleanName.contains("parasol") || cleanName.contains("raincoat") -> Icons.Default.Umbrella
        cleanName.contains("document") || cleanName.contains("paper") || cleanName.contains("invoice") || cleanName.contains("receipt") || cleanName.contains("bill") || cleanName.contains("contract") || cleanName.contains("certificate") || cleanName.contains("tax") || cleanName.contains("form") || cleanName.contains("letter") || cleanName.contains("envelope") || cleanName.contains("mail") || cleanName.contains("post") -> Icons.Default.Description
        cleanName.contains("card") || cleanName.contains("credit") || cleanName.contains("debit") || cleanName.contains("visa") || cleanName.contains("mastercard") || cleanName.contains("amex") || cleanName.contains("giftcard") || cleanName.contains("gift card") || cleanName.contains("storecard") -> Icons.Default.CreditCard
        cleanName.contains("money") || cleanName.contains("cash") || cleanName.contains("coin") || cleanName.contains("coins") || cleanName.contains("dollar") || cleanName.contains("rupee") || cleanName.contains("euro") || cleanName.contains("yen") || cleanName.contains("wealth") || cleanName.contains("funds") -> Icons.Default.MonetizationOn
        cleanName.contains("box") || cleanName.contains("package") || cleanName.contains("carton") || cleanName.contains("parcel") || cleanName.contains("shipment") || cleanName.contains("delivery") || cleanName.contains("crate") || cleanName.contains("storage") -> Icons.Default.Inventory2
        cleanName.contains("bank") || cleanName.contains("atm") || cleanName.contains("checking") || cleanName.contains("savings") -> Icons.Default.AccountBalance
        cleanName.contains("store") || cleanName.contains("shop") || cleanName.contains("market") || cleanName.contains("boutique") || cleanName.contains("grocer") -> Icons.Default.Store
        cleanName.contains("school") || cleanName.contains("college") || cleanName.contains("university") || cleanName.contains("class") || cleanName.contains("classroom") || cleanName.contains("academy") || cleanName.contains("institute") -> Icons.Default.School
        cleanName.contains("pill") || cleanName.contains("medicine") || cleanName.contains("tablet") || cleanName.contains("capsule") || cleanName.contains("drug") || cleanName.contains("prescription") || cleanName.contains("firstaid") || cleanName.contains("first aid") || cleanName.contains("medical") || cleanName.contains("doctor") || cleanName.contains("hospital") || cleanName.contains("clinic") -> Icons.Default.MedicalServices
        cleanName.contains("mask") || cleanName.contains("masks") || cleanName.contains("respirator") -> Icons.Default.Masks
        cleanName.contains("vaccine") || cleanName.contains("syringe") || cleanName.contains("injection") || cleanName.contains("shot") || cleanName.contains("booster") -> Icons.Default.Vaccines
        cleanName.contains("soap") || cleanName.contains("shampoo") || cleanName.contains("shaving") || cleanName.contains("toothbrush") || cleanName.contains("toothpaste") || cleanName.contains("comb") || cleanName.contains("towel") || cleanName.contains("conditioner") || cleanName.contains("loofah") || cleanName.contains("deodorant") -> Icons.Default.Bathtub
        cleanName.contains("perfume") || cleanName.contains("makeup") || cleanName.contains("lipstick") || cleanName.contains("lotion") || cleanName.contains("cream") || cleanName.contains("beauty") || cleanName.contains("cosmetic") || cleanName.contains("scent") || cleanName.contains("cologne") -> Icons.Default.Face
        cleanName.contains("plant") || cleanName.contains("flower") || cleanName.contains("leaf") || cleanName.contains("tree") || cleanName.contains("garden") || cleanName.contains("herbs") || cleanName.contains("rose") || cleanName.contains("shrub") || cleanName.contains("flora") || cleanName.contains("seed") -> Icons.Default.LocalFlorist
        cleanName.contains("pet") || cleanName.contains("dog") || cleanName.contains("cat") || cleanName.contains("puppy") || cleanName.contains("kitten") || cleanName.contains("hamster") || cleanName.contains("rabbit") || cleanName.contains("bird") || cleanName.contains("fish") || cleanName.contains("paw") || cleanName.contains("animal") || cleanName.contains("veterinary") -> Icons.Default.Pets
        cleanName.contains("sun") || cleanName.contains("sunny") || cleanName.contains("solar") -> Icons.Default.WbSunny
        cleanName.contains("cloud") || cleanName.contains("cloudy") -> Icons.Default.Cloud
        cleanName.contains("moon") || cleanName.contains("night") -> Icons.Default.DarkMode
        cleanName.contains("star") || cleanName.contains("stars") || cleanName.contains("rating") -> Icons.Default.Star
        else -> getCategoryIconVector(categoryIconName)
    }
}

@Composable
fun getSpaceIconVector(iconName: String?): ImageVector {
    return when (iconName) {
        "home" -> Icons.Default.Home
        "bedroom" -> Icons.Default.Bed
        "kitchen" -> Icons.Default.Kitchen
        "bathroom" -> Icons.Default.Bathtub
        "garage" -> Icons.Default.Garage
        "office" -> Icons.Default.Business
        "warehouse" -> Icons.Default.Warehouse
        "store" -> Icons.Default.Store
        "workshop" -> Icons.Default.Build
        "garden" -> Icons.Default.LocalFlorist
        "car" -> Icons.Default.DirectionsCar
        "inbox" -> Icons.Default.Inbox
        "box" -> Icons.Default.Inventory2
        "drawer" -> Icons.Default.ViewStream
        "closet" -> Icons.Default.DoorSliding
        "shelf" -> Icons.Default.ViewAgenda
        "cabinet" -> Icons.Default.Inventory
        "backpack" -> Icons.Default.Work
        "suitcase" -> Icons.Default.Luggage
        "safe" -> Icons.Default.Security
        "archive" -> Icons.Default.Archive
        "devices" -> Icons.Default.Devices
        "lock" -> Icons.Default.Lock
        else -> Icons.Default.Inbox
    }
}

fun getSpaceIconLabel(iconName: String?): String {
    return when (iconName) {
        "home" -> "Home"
        "bedroom" -> "Bedroom"
        "kitchen" -> "Kitchen"
        "bathroom" -> "Bathroom"
        "garage" -> "Garage"
        "office" -> "Office"
        "warehouse" -> "Warehouse"
        "store" -> "Store"
        "workshop" -> "Workshop"
        "garden" -> "Garden"
        "car" -> "Car"
        "inbox" -> "Inbox"
        "box" -> "Box"
        "drawer" -> "Drawer"
        "closet" -> "Closet"
        "shelf" -> "Shelf"
        "cabinet" -> "Cabinet"
        "backpack" -> "Backpack"
        "suitcase" -> "Suitcase"
        "safe" -> "Safe"
        "archive" -> "Archive"
        "devices" -> "Devices"
        "lock" -> "Lock"
        else -> "Default"
    }
}

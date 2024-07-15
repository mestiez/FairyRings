package com.zooi;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

public class Stories {
    public static void apply(ItemStack stack){

        var bookTag = new NbtCompound();
        bookTag.putBoolean("resolved", false);
        bookTag.putString("title", "Lament");
        bookTag.putString("author", "Unknown");
        bookTag.putInt("generation", 3);
        var pages = new NbtList();
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("I came across a most peculiar ring of flower petals glowing softly under the moonlight. Legends often spoke of such rings, woven by the delicate hands of the fey. My curiosity was piqued, and in my arrogance, I sought to uncover their mysteries."))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("Without much thought, I seized a petal, breaking the sanctity of the circle. The air trembled with a whisper most dreadful, and a chill gripped my very bones. A foreboding presence settled upon me, darkening my spirit. An omen had been set in motion."))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("Since then, a string of cursed events has plagued me. As I lay to rest, I found sleep only in unfamiliar places. Each morning demanded that I find new quarters. Craftsmanship and construction became my only reprieve from the curse shadowing my every step."))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("An unsettling affliction has seized my body; unbidden sounds plague my ears, whispers and creakings unheard by others. Foods I consume turn vile, hunger and sickness making my days unbearable."))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("Hauntings now follow my every step. Candles extinguish on their own volition, doors swing open by an unseen hand, and objects move as though alive. I am most assuredly pursued by an unseen, malicious force."))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("My loyal friend growls with anger and does not recognize me. It looks upon me as though I am a stranger. The hearthfires now rage uncontrollably, threatening my shelter and leaving me adrift."))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("Life itself flees from my presence. Grass withers beneath my feet; flowers close their petals in fear. Crops on which I rely for sustenance rot and decay."))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("The very provisions that sustain me have turned foul. The meat that once sated my hunger is now rank flesh. My situation grows dire."))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("Oh, would that I had heeded the warnings of the old wives and left that fateful ring undisturbed. To any soul who finds this tome, take heed and disturb not the rings of the fey."))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("For in their delicate dance lies the balance of our world, a balance that even the wisest among us should not dare tip."))));
        pages.add(NbtString.of(Text.Serializer.toJson(Text.of("My story serves as a grim warning and a plea: respect that which you do not understand."))));
        bookTag.put("pages", pages);
        bookTag.put("filtered_pages", pages);
        stack.setNbt(bookTag);
    }
}

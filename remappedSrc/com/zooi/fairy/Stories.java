package com.zooi.fairy;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class Stories {
    public static void apply(ItemStack stack){
        var pages = new ArrayList<RawFilteredPair<Text>>();

        pages.add(RawFilteredPair.of(Text.of("I came across a most peculiar ring of flower petals glowing softly under the moonlight. Legends often spoke of such rings, woven by the delicate hands of the fey. My curiosity was piqued, and in my arrogance, I sought to uncover their mysteries.")));
        pages.add(RawFilteredPair.of(Text.of("Without much thought, I seized a petal, breaking the sanctity of the circle. The air trembled with a whisper most dreadful, and a chill gripped my very bones. A foreboding presence settled upon me, darkening my spirit. An omen had been set in motion.")));
        pages.add(RawFilteredPair.of(Text.of("Since then, a string of cursed events has plagued me. As I lay to rest, I found sleep only in unfamiliar places. Each morning demanded that I find new quarters. Craftsmanship and construction became my only reprieve from the curse shadowing my every step.")));
        pages.add(RawFilteredPair.of(Text.of("An unsettling affliction has seized my body; unbidden sounds plague my ears, whispers and creakings unheard by others. Foods I consume turn vile, hunger and sickness making my days unbearable.")));
        pages.add(RawFilteredPair.of(Text.of("Hauntings now follow my every step. Candles extinguish on their own volition, doors swing open by an unseen hand, and objects move as though alive. I am most assuredly pursued by an unseen, malicious force.")));
        pages.add(RawFilteredPair.of(Text.of("My loyal friend growls with anger and does not recognize me. It looks upon me as though I am a stranger. The hearthfires now rage uncontrollably, threatening my shelter and leaving me adrift.")));
        pages.add(RawFilteredPair.of(Text.of("Life itself flees from my presence. Grass withers beneath my feet; flowers close their petals in fear. Crops on which I rely for sustenance rot and decay.")));
        pages.add(RawFilteredPair.of(Text.of("The very provisions that sustain me have turned foul. The meat that once sated my hunger is now rank flesh. My situation grows dire.")));
        pages.add(RawFilteredPair.of(Text.of("Oh, would that I had heeded the warnings of the old wives and left that fateful ring undisturbed. To any soul who finds this tome, take heed and disturb not the rings of the fey.")));
        pages.add(RawFilteredPair.of(Text.of("For in their delicate dance lies the balance of our world, a balance that even the wisest among us should not dare tip.")));
        pages.add(RawFilteredPair.of(Text.of("My story serves as a grim warning and a plea: respect that which you do not understand.")));

        var book = new WrittenBookContentComponent(RawFilteredPair.of("Lament"), "Unknown", 3, pages, true);

        stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, book);
    }
}

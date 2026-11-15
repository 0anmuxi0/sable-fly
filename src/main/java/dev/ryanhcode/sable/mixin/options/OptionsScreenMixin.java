package dev.ryanhcode.sable.mixin.options;

import dev.ryanhcode.sable.config.SubLevelSettingsScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({OptionsScreen.class})
public abstract class OptionsScreenMixin extends Screen {
   @Shadow
   @Final
   private Options options;

   protected OptionsScreenMixin(Component var1) {
      super(var1);
   }

   @Inject(
      method = {"createOnlineButton"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void sable$createSableButton(CallbackInfoReturnable<LayoutElement> var1) {
      if (this.minecraft.level != null && this.minecraft.hasSingleplayerServer()) {
         LinearLayout var2 = LinearLayout.vertical();
         Button var3 = Button.builder(
               SubLevelSettingsScreen.TITLE,
               var1x -> this.minecraft.gui.setScreen(new SubLevelSettingsScreen(this, this.options, SubLevelSettingsScreen.TITLE))
            )
            .pos(0, 30)
            .size(150, 20)
            .build();
         var2.addChild((LayoutElement)var1.getReturnValue());
         var2.spacing(5);
         var2.addChild(var3);
         var1.setReturnValue(var2);
      }
   }
}

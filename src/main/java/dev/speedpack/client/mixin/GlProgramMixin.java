package dev.speedpack.client.mixin;

//? if >=26.3 {
import java.util.List;

import com.mojang.renderpearl.backend.opengl.GlProgram;
import com.mojang.renderpearl.backend.opengl.GlShaderModule;
//?} else {
/*import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.vertex.VertexFormat;
*///?}
import dev.speedpack.client.gl.ProgramBinaryCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlProgram.class)
public abstract class GlProgramMixin {
	//? if >=26.3 {
	@Inject(method = "link", at = @At("HEAD"))
	private static void speedpack$begin(List<GlShaderModule> modules, String label, CallbackInfoReturnable<GlProgram> cir) {
		ProgramBinaryCache.begin(modules, label);
	}

	@Redirect(method = "link", at = @At(value = "INVOKE",
		target = "Lcom/mojang/renderpearl/backend/opengl/GlStateManager;glLinkProgram(I)V"))
	private static void speedpack$link(int program) {
		ProgramBinaryCache.link(program);
	}
	//?} else if >=26.2 {
	/*@Inject(method = "link", at = @At("HEAD"))
	private static void speedpack$begin(GlShaderModule vertex, GlShaderModule fragment, VertexFormat[] vertexFormats,
		String label, CallbackInfoReturnable<GlProgram> cir) {
		ProgramBinaryCache.begin(vertex, fragment, vertexFormats, label);
	}

	@Redirect(method = "link", at = @At(value = "INVOKE",
		target = "Lcom/mojang/blaze3d/opengl/GlStateManager;glLinkProgram(I)V"))
	private static void speedpack$link(int program) {
		ProgramBinaryCache.link(program);
	}
	*///?} else {
	/*@Inject(method = "link", at = @At("HEAD"))
	private static void speedpack$begin(GlShaderModule vertex, GlShaderModule fragment, VertexFormat vertexFormat,
		String label, CallbackInfoReturnable<GlProgram> cir) {
		ProgramBinaryCache.begin(vertex, fragment, new VertexFormat[] {vertexFormat}, label);
	}

	@Redirect(method = "link", at = @At(value = "INVOKE",
		target = "Lcom/mojang/blaze3d/opengl/GlStateManager;glLinkProgram(I)V"))
	private static void speedpack$link(int program) {
		ProgramBinaryCache.link(program);
	}
	*///?}
}

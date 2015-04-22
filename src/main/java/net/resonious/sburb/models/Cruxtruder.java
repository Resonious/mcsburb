// Date: 6/20/2014 9:21:01 PM
// Template version 1.1
// Java generated by Techne
// Keep in mind that you still need to fill in some blanks
// - ZeuX

package net.resonious.sburb.models;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class Cruxtruder extends ModelBase
{
  //fields
    ModelRenderer Shape1;
    ModelRenderer Shape2;
    ModelRenderer Shape3;
    ModelRenderer Shape5;
    ModelRenderer Shape52;
    ModelRenderer Shape53;
    ModelRenderer Shape54;
    ModelRenderer Shape42;
    ModelRenderer Shape43;
    ModelRenderer Shape44;
    ModelRenderer Shape4;
    ModelRenderer Shape6;
    ModelRenderer Shape62;
    ModelRenderer Shape63;
    ModelRenderer Shape64;
    ModelRenderer Piece1;
  
  public Cruxtruder()
  {
    textureWidth = 137;
    textureHeight = 100;
    
      Shape1 = new ModelRenderer(this, 0, 55);
      Shape1.addBox(0F, 15F, 0F, 32, 11, 32);
      Shape1.setRotationPoint(-8F, -2F, -8F);
      Shape1.setTextureSize(137, 100);
      Shape1.mirror = true;
      setRotation(Shape1, 0F, 0F, 0F);
      Shape2 = new ModelRenderer(this, 0, 33);
      Shape2.addBox(0F, 0F, 0F, 16, 5, 16);
      Shape2.setRotationPoint(0F, 8F, 0F);
      Shape2.setTextureSize(137, 100);
      Shape2.mirror = true;
      setRotation(Shape2, 0F, 0F, 0F);
      Shape3 = new ModelRenderer(this, 0, 0);
      Shape3.addBox(0F, 0F, 0F, 4, 10, 4);
      Shape3.setRotationPoint(6F, -2F, 6F);
      Shape3.setTextureSize(137, 100);
      Shape3.mirror = true;
      setRotation(Shape3, 0F, 0F, 0F);
      Shape5 = new ModelRenderer(this, 0, 19);
      Shape5.addBox(0F, 0F, 0F, 6, 4, 2);
      Shape5.setRotationPoint(5F, 9F, -2F);
      Shape5.setTextureSize(137, 100);
      Shape5.mirror = true;
      setRotation(Shape5, 0F, 0F, 0F);
      Shape52 = new ModelRenderer(this, 0, 19);
      Shape52.addBox(0F, 0F, 0F, 6, 4, 2);
      Shape52.setRotationPoint(-2F, 9F, 11F);
      Shape52.setTextureSize(137, 100);
      Shape52.mirror = true;
      setRotation(Shape52, 0F, 1.570796F, 0F);
      Shape53 = new ModelRenderer(this, 0, 19);
      Shape53.addBox(0F, 0F, 0F, 6, 4, 2);
      Shape53.setRotationPoint(16F, 9F, 11F);
      Shape53.setTextureSize(137, 100);
      Shape53.mirror = true;
      setRotation(Shape53, 0F, 1.570796F, 0F);
      Shape54 = new ModelRenderer(this, 0, 19);
      Shape54.addBox(0F, 0F, 0F, 6, 4, 2);
      Shape54.setRotationPoint(5F, 9F, 16F);
      Shape54.setTextureSize(137, 100);
      Shape54.mirror = true;
      setRotation(Shape54, 0F, 0F, 0F);
      Shape42 = new ModelRenderer(this, 0, 12);
      Shape42.addBox(0F, 0F, 0F, 6, 3, 1);
      Shape42.setRotationPoint(5F, 10F, -3F);
      Shape42.setTextureSize(137, 100);
      Shape42.mirror = true;
      setRotation(Shape42, 0F, 0F, 0F);
      Shape43 = new ModelRenderer(this, 0, 12);
      Shape43.addBox(0F, 0F, 0F, 6, 3, 1);
      Shape43.setRotationPoint(5F, 10F, 18F);
      Shape43.setTextureSize(137, 100);
      Shape43.mirror = true;
      setRotation(Shape43, 0F, 0F, 0F);
      Shape44 = new ModelRenderer(this, 0, 12);
      Shape44.addBox(0F, 0F, 0F, 6, 3, 1);
      Shape44.setRotationPoint(-3F, 10F, 11F);
      Shape44.setTextureSize(137, 100);
      Shape44.mirror = true;
      setRotation(Shape44, 0F, 1.570796F, 0F);
      Shape4 = new ModelRenderer(this, 0, 12);
      Shape4.addBox(0F, 0F, 0F, 6, 3, 1);
      Shape4.setRotationPoint(18F, 10F, 11F);
      Shape4.setTextureSize(137, 100);
      Shape4.mirror = true;
      setRotation(Shape4, 0F, 1.570796F, 0F);
      Shape6 = new ModelRenderer(this, 0, 7);
      Shape6.addBox(0F, 0F, 0F, 6, 2, 1);
      Shape6.setRotationPoint(5F, 11F, -4F);
      Shape6.setTextureSize(137, 100);
      Shape6.mirror = true;
      setRotation(Shape6, 0F, 0F, 0F);
      Shape62 = new ModelRenderer(this, 0, 7);
      Shape62.addBox(0F, 0F, 0F, 6, 2, 1);
      Shape62.setRotationPoint(19F, 11F, 11F);
      Shape62.setTextureSize(137, 100);
      Shape62.mirror = true;
      setRotation(Shape62, 0F, 1.570796F, 0F);
      Shape63 = new ModelRenderer(this, 0, 7);
      Shape63.addBox(0F, 0F, 0F, 6, 2, 1);
      Shape63.setRotationPoint(-4F, 11F, 11F);
      Shape63.setTextureSize(137, 100);
      Shape63.mirror = true;
      setRotation(Shape63, 0F, 1.570796F, 0F);
      Shape64 = new ModelRenderer(this, 0, 7);
      Shape64.addBox(0F, 0F, 0F, 6, 2, 1);
      Shape64.setRotationPoint(5F, 11F, 19F);
      Shape64.setTextureSize(137, 100);
      Shape64.mirror = true;
      setRotation(Shape64, 0F, 0F, 0F);
    Piece1 = new ModelRenderer(this, "Piece1");
    Piece1.setRotationPoint(0F, 0F, 0F);
    setRotation(Piece1, 0F, 0F, 0F);
    Piece1.mirror = true;
  }
  
  public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
  {
    super.render(entity, f, f1, f2, f3, f4, f5);
    setRotationAngles(f, f1, f2, f3, f4, f5, entity);
    Shape1.render(f5);
    Shape2.render(f5);
    Shape3.render(f5);
    Shape5.render(f5);
    Shape52.render(f5);
    Shape53.render(f5);
    Shape54.render(f5);
    Shape42.render(f5);
    Shape43.render(f5);
    Shape44.render(f5);
    Shape4.render(f5);
    Shape6.render(f5);
    Shape62.render(f5);
    Shape63.render(f5);
    Shape64.render(f5);
    Piece1.render(f5);
  }
  
  private void setRotation(ModelRenderer model, float x, float y, float z)
  {
    model.rotateAngleX = x;
    model.rotateAngleY = y;
    model.rotateAngleZ = z;
  }
  
  public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity e)
  {
    super.setRotationAngles(f, f1, f2, f3, f4, f5, e);
  }

}

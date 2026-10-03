package com.bistroops.meal.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;


@Converter(autoApply = true)
public class MealEnumConverter implements AttributeConverter<MealEnum, String>{

	@Override
	public String convertToDatabaseColumn(MealEnum attribute) {
		return attribute != null ? attribute.getMealStatusEn() : null;
	}

	@Override
	public MealEnum convertToEntityAttribute(String dbData) {
		return dbData != null ? MealEnum.fromMealEnum(dbData) : null;
	}
}

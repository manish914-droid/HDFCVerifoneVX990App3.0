package com.example.verifonevx990app.brandemi

import java.io.Serializable

class BrandEMIDataModal : Serializable {
    private var brandID: String? = null
    private var brandName: String? = null
    private var brandReservedValues: String? = null
    private var categoryID: String? = null
    private var categoryName: String? = null
    private var productID: String? = null
    private var productName: String? = null
    private var childSubCategoryID: String? = null
    private var childSubCategoryName: String? = null
    private var validationTypeName: String? = null
    private var isRequired: String? = null
    private var inputDataType: String? = null
    private var minLength: String? = null
    private var maxLength: String? = null
    private var productMinAmount: String? = null
    private var productMaxAmount: String? = null
    private var dataTimeStampChangedOrNot: Boolean = false

    fun setBrandID(brID: String) {
        this.brandID = brID
    }

    fun getBrandID(): String? {
        return brandID
    }

    fun setBrandName(brName: String) {
        this.brandName = brName
    }

    fun getBrandName(): String? {
        return brandName
    }

    fun setBrandReservedValue(brReservedValue: String) {
        this.brandReservedValues = brReservedValue
    }

    fun getBrandReservedValue(): String? {
        return brandReservedValues
    }

    fun setCategoryID(catID: String) {
        this.categoryID = catID
    }

    fun getCategoryID(): String? {
        return categoryID
    }

    fun setCategoryName(catName: String) {
        this.categoryName = catName
    }

    fun getCategoryName(): String? {
        return categoryName
    }

    fun setProductID(proID: String) {
        this.productID = proID
    }

    fun getProductID(): String? {
        return productID
    }

    fun setProductName(proName: String) {
        this.productName = proName
    }

    fun getProductName(): String? {
        return productName
    }

    fun setChildSubCategoryID(childCatID: String) {
        this.childSubCategoryID = childCatID
    }

    fun getChildSubCategoryID(): String? {
        return childSubCategoryID
    }

    fun setChildSubCategoryName(childCatName: String) {
        this.childSubCategoryName = childCatName
    }

    fun getChildSubCategoryName(): String? {
        return childSubCategoryName
    }

    fun setValidationTypeName(validationTName: String) {
        this.validationTypeName = validationTName
    }

    fun getValidationTypeName(): String? {
        return validationTypeName
    }

    fun setIsRequired(required: String) {
        this.isRequired = required
    }

    fun getIsRequired(): String? {
        return isRequired
    }

    fun setInputDataType(inputDType: String) {
        this.inputDataType = inputDType
    }

    fun getInputDataType(): String? {
        return inputDataType
    }

    fun setMinLength(length: String) {
        this.minLength = length
    }

    fun getMinLength(): String? {
        return minLength
    }

    fun setMaxLength(length: String) {
        this.maxLength = length
    }

    fun getMaxLength(): String? {
        return maxLength
    }

    fun setProductMinAmount(amount: String) {
        this.productMinAmount = amount
    }

    fun getProductMinAmount(): String? {
        return productMinAmount
    }

    fun setProductMaxAmount(amount: String) {
        this.productMaxAmount = amount
    }

    fun getProductMaxAmount(): String? {
        return productMaxAmount
    }

    fun setDataTimeStampChangedOrNot(timeChangeStatus: Boolean) {
        this.dataTimeStampChangedOrNot = timeChangeStatus
    }

    fun getDataTimeStampChangedOrNot(): Boolean {
        return dataTimeStampChangedOrNot
    }
}
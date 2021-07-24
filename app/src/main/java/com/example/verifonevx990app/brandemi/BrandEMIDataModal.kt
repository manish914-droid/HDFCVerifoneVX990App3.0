package com.example.verifonevx990app.brandemi

import java.io.Serializable

class BrandEMIDataModal : Serializable {
 var brandID: String? = null
 var brandName: String? = null
 var brandReservedValues: String? = null
 var categoryID: String? = null
 var categoryName: String? = null
 var productID: String? = null
 var productName: String? = null
 var childSubCategoryID: String? = null
 var childSubCategoryName: String? = null
 var validationTypeName: String? = null
 var isRequired: String? = null
 var inputDataType: String? = null
 var minLength: String? = null
 var maxLength: String? = null
 var productMinAmount: String? = null
 var productMaxAmount: String? = null
 var dataTimeStampChangedOrNot: Boolean = false

     var imeiORserailNum:String=""


}
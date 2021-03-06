<table class="osafe">
    <tr class="heading">
        <th class="actionOrderCol firstCol selectOrderItem">
            <#-- <input type="checkbox" class="checkBoxEntry" name="orderItemSeqIdall" id="orderItemSeqIdall" value="Y" onclick="javascript:setCheckboxes('${detailFormName!""}','orderItemSeqId')"  <#if parameters.orderItemSeqIdall?has_content>checked</#if>/> -->
        </th>
        <th class="idCol">${uiLabelMap.ItemSeqNoLabel}</th>
        <th class="idCol">${uiLabelMap.ProductNoLabel}</th>
        <th class="nameCol">${uiLabelMap.ProductNameLabel}</th>
        <th class="statusCol">${uiLabelMap.ItemStatusLabel}</th>
        <th class="nameCol">${uiLabelMap.OrderQtyLabel}</th>
        <th class="nameCol">${uiLabelMap.ReturnedQtyLabel}</th>
        <th class="nameCol">${uiLabelMap.NewQtyLabel}</th>
        <th class="nameCol">${uiLabelMap.ReturningQtyLabel}</th>
        <th class="nameCol">${uiLabelMap.ReturnReasonLabel}</th>
        <th class="nameCol">${uiLabelMap.CarrierLabel}</th>
        <th class="dateCol">${uiLabelMap.ShipDateLabel}</th>
        <th class="nameCol lastCol">${uiLabelMap.TrackingLabel}</th>
    </tr>
    <#if orderItems?exists && orderItems?has_content>
        <#assign rowClass = "1">
        <#assign rowNo = 0/>
        <#list orderItems as orderItem>
            <#assign productId = orderItem.productId?if_exists>
            <#assign itemProduct = orderItem.getRelatedOne("Product")/>
            <#assign itemStatus = orderItem.getRelatedOne("StatusItem")/>
            <#assign productContentWrapper = Static["org.ofbiz.product.product.ProductContentWrapper"].makeProductContentWrapper(itemProduct,request)>
            <#assign productName = productContentWrapper.get("PRODUCT_NAME")!itemProduct.productName!"">
            <#assign shipGroupAssoc = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(delegator.findByAnd("OrderItemShipGroupAssoc", {"orderId": orderItem.orderId, "orderItemSeqId": orderItem.orderItemSeqId}))! />
            <#if shipGroupAssoc?has_content>
                <#assign shipGroup = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(delegator.findByAnd("OrderItemShipGroup", {"orderId": orderItem.orderId, "shipGroupSeqId": shipGroupAssoc.shipGroupSeqId}))/>
                <#if shipGroup?has_content>
                    <#assign shipDate = ""/>
                    <#assign orderHeader = delegator.findByPrimaryKey("OrderHeader", {"orderId": orderItem.orderId})/>
                    <#if orderHeader?has_content && (orderHeader.statusId == "ORDER_COMPLETED" || orderItem.statusId == "ITEM_COMPLETED") >
                        <#assign shipDate = shipGroup.estimatedShipDate!""/>
                        <#if shipDate?has_content>
                            <#assign shipDate = shipDate?string(preferredDateFormat)!""/>
                        </#if>
                    </#if>
                    <#assign trackingNumber = shipGroup.trackingNumber!""/>
                    <#assign findCarrierShipmentMethodMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("shipmentMethodTypeId", shipGroup.shipmentMethodTypeId, "partyId", shipGroup.carrierPartyId,"roleTypeId" ,"CARRIER")>
                    <#assign carrierShipmentMethod = delegator.findByPrimaryKey("CarrierShipmentMethod", findCarrierShipmentMethodMap)>
                    <#assign shipmentMethodType = carrierShipmentMethod.getRelatedOne("ShipmentMethodType")/>
                    <#assign description = shipmentMethodType.description!""/>
                    <#assign carrierPartyGroupName = ""/>
                    <#if shipGroup.carrierPartyId != "_NA_">
                        <#assign carrierParty = carrierShipmentMethod.getRelatedOne("Party")/>
                        <#assign carrierPartyGroup = carrierParty.getRelatedOne("PartyGroup")/>
                        <#assign carrierPartyGroupName = carrierPartyGroup.groupName/>
                    </#if>
                </#if>
            </#if>
            <tr class="dataRow <#if rowClass == "2">even<#else>odd></#if>">
                  
                    <td class="actionOrderCol <#if !orderItem_has_next>lastRow</#if> firstCol">
                      <#if orderItem.statusId == "ITEM_APPROVED">
                        <div class="statusChangeOrderCheckbox">
                          <input type="checkbox" class="checkBoxEntry" name="orderItemSeqId-${orderItem_index}" id="orderItemSeqId-${rowNo}" value="${orderItem.orderItemSeqId!}" <#if parameters.get("orderItemSeqId-${rowNo}")?has_content>checked</#if> onchange="javascript:getOrderRefundData();"/>
                        </div>
                      </#if>
                      <#if orderItem.statusId == "ITEM_COMPLETED">
                        <#assign returnItems = orderItem.getRelated("ReturnItem") />
                        <#assign returnedQty = 0 />
                        <#list returnItems as returnItem>
                          <#assign returnedQty = returnedQty + returnItem.returnQuantity! />
                        </#list>
                        <#if (returnedQty < orderItem.quantity)>
                          <div class="productReturnOrderCheckbox">
                            <input type="checkbox" class="checkBoxEntry" name="orderItemSeqId-${orderItem_index}" id="orderItemSeqId-${rowNo}" value="${orderItem.orderItemSeqId!}" <#if parameters.get("orderItemSeqId-${rowNo}")?has_content>checked</#if> onchange="javascript:getOrderRefundData();"/>
                          </div>
                        </#if>
                      </#if>
                    </td>
                
                <#-- <td class="actionOrderCol <#if !orderItem_has_next>lastRow</#if> firstCol selectOrderItem">
                  <input type="checkbox" class="checkBoxEntry" name="orderItemSeqId-${orderItem_index}" id="orderItemSeqId-${rowNo}" value="${orderItem.orderItemSeqId!}" <#if parameters.get("orderItemSeqId-${rowNo}")?has_content>checked</#if>/>
                </td> -->
                
                
                <td class="itemCol <#if !orderItem_has_next>lastRow</#if>"><a href="<@ofbizUrl>orderItemDetail?orderId=${orderItem.orderId!}</@ofbizUrl>">${(orderItem.orderItemSeqId)!""}</a></td>
                <td class="idCol <#if !orderItem_has_next>lastRow</#if>">
                  <#if itemProduct?has_content && itemProduct.isVirtual == 'Y'>
                    <a href="<@ofbizUrl>virtualProductDetail?productId=${itemProduct.productId!}</@ofbizUrl>">${itemProduct.productId!"N/A"}</a>
                  <#elseif itemProduct?has_content && itemProduct.isVariant == 'Y'>
                    <a href="<@ofbizUrl>variantProductDetail?productId=${itemProduct.productId!}</@ofbizUrl>">${itemProduct.productId!"N/A"}</a>
                  <#elseif itemProduct?has_content && itemProduct.isVirtual == 'N' && itemProduct.isVariant == 'N'>
                    <a href="<@ofbizUrl>finishedProductDetail?productId=${itemProduct.productId!}</@ofbizUrl>">${itemProduct.productId!"N/A"}</a>
                  </#if>
                </td>
                <td class="nameCol <#if !orderItem_has_next>lastRow</#if>">${productName?if_exists}</td>
                <td class="statusCol <#if !orderItem_has_next>lastRow</#if>">${itemStatus.get("description",locale)}</td>
                <td class="nameCol <#if !orderItem_has_next>lastRow</#if>">${orderItem.quantity!} </td>
                <td class="nameCol <#if !orderItem_has_next>lastRow</#if>">${returnedQty!} </td>
                
                <td class="nameCol <#if !orderItem_has_next>lastRow</#if>">
                  <div class="orderItemNewQty">
                    <#if (orderItem.statusId == "ITEM_APPROVED")>
                      <input type="text" class="small" name="newQty_${orderItem_index}" id="newQty_${orderItem_index}" />
                    <#else>
                      &nbsp;
                    </#if>
                  </div>
                </td>
                <td class="nameCol <#if !orderItem_has_next>lastRow</#if>">
                  <div class="orderItemReturningQty">
                    <#if (orderItem.statusId == "ITEM_COMPLETED") && (returnedQty < orderItem.quantity)>
                      <input type="hidden" value="${orderItem.unitPrice!}" class="small" name="returnPrice_${orderItem_index}" id="returnPrice_${orderItem_index}" />
                      <input type="text" class="small" name="returnQuantity_${orderItem_index}" id="returnQuantity_${orderItem_index}" />
                    <#else>
                      &nbsp;
                    </#if>
                  </div>
                </td>
                <td class="nameCol">
                  <div class="orderItemReturnReason">
                    <#if (orderItem.statusId == "ITEM_COMPLETED") && (returnedQty < orderItem.quantity)>
                      <select name="returnReasonId_${orderItem_index}" id="returnReasonId_${orderItem_index}">
                        <#list returnReasons as reason>
                          <option value="${reason.returnReasonId}">${reason.description!reason.returnReasonId!}</option>
                        </#list>
                      </select>
                    </#if>
                  </div>
                </td>
                
                <td class="nameCol <#if !orderItem_has_next>lastRow</#if>">${carrierPartyGroupName!} ${description!}</td>
                <td class="dateCol <#if !orderItem_has_next>lastRow</#if>">${shipDate!}</td>
                <td class="nameCol <#if !orderItem_has_next>lastRow</#if> lastCol">${trackingNumber!}</td>
            </tr>
            <#-- toggle the row color -->
            <#if rowClass == "2">
                <#assign rowClass = "1">
            <#else>
                <#assign rowClass = "2">
            </#if>
            <#assign rowNo = rowNo+1/>
        </#list>
    </#if>
</table>
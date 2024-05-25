package com.example.dormproject.retrofit.req.repair

data class ReqRepairGetRepairByIdResponse(
    val guestRequestList: List<ReqRepairGetRepairByIdListItem>
)

data class ReqRepairGetRepairByIdListItem(
    val reqId: Int,
    val title: String,
    val host: Int,
    val responsible: Int,
    val typeId: Int,
    val description: String,
    val statusId: Int
)
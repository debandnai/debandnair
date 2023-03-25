package com.salonsolution.app.data.paging

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.gson.JsonObject
import com.salonsolution.app.data.model.FoodList
import com.salonsolution.app.data.network.UserApi
import com.salonsolution.app.utils.Constants.TOTAL_NO_OF_PRODUCTS_PER_PAGE
import retrofit2.HttpException
import java.io.IOException

class FoodListPagingSource(
    private val userApi: UserApi,
    private val foodListRequest: JsonObject,
    _totalFoodCountLiveData: MutableLiveData<String>
) : PagingSource<Int, FoodList>() {
    private val totalListCount = _totalFoodCountLiveData

    override fun getRefreshKey(state: PagingState<Int, FoodList>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FoodList> {
        return try {
            val position = params.key ?: 1
            val startPositionItemNumber = (position-1) * TOTAL_NO_OF_PRODUCTS_PER_PAGE
            foodListRequest.addProperty("first", startPositionItemNumber)
            Log.d("tag", "paging request: $foodListRequest")
            val response = userApi.foodList(foodListRequest)
            val mData = response.body()?.response?.data
            val totalCount = mData?.totalCount ?: 0
            totalListCount.postValue(totalCount.toString())
            Log.d("tag", "load: $position..")
            val loadedItemCount = position* TOTAL_NO_OF_PRODUCTS_PER_PAGE
            val prevKey = if (position > 1) position - 1 else null
            val nextKey = if (loadedItemCount < totalCount) position + 1 else null
            LoadResult.Page(
                data = mData?.foodList?: ArrayList(),
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: IOException) {
            Log.d("tag", "IOException: ${e.message}..")
            LoadResult.Error(e)
        } catch (e: HttpException) {
            Log.d("tag", "HttpException: ${e.message}..")
            LoadResult.Error(e)
        }
    }


}
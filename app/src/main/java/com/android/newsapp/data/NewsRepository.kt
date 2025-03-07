package com.android.newsapp.data

import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.room.util.newStringBuilder
import com.android.newsapp.BuildConfig
import com.android.newsapp.data.local.entity.NewsEntity
import com.android.newsapp.data.local.room.NewsDao
import com.android.newsapp.data.remote.response.NewsResponse
import com.android.newsapp.data.remote.retrofit.ApiService
import com.android.newsapp.utils.AppExecutors
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NewsRepository private constructor(
    private val apiService: ApiService,
    private val newsDao: NewsDao,
    private val appExecutors: AppExecutors
) {
    fun getHeadlineNews(): LiveData<Result<List<NewsEntity>>> = liveData {
        emit(Result.Loading)
        try {
            val response = apiService.getNews(BuildConfig.API_KEY)
            val articles = response.articles
            val newList = articles.map { articles ->
                val isBookmarked = newsDao.isNewsBookmarked(articles.title)
                NewsEntity(
                    articles.title,
                    articles.publishedAt,
                    articles.urlToImage,
                    articles.url,
                    isBookmarked
                )
            }
            newsDao.deleteAll()
            newsDao.insertNews(newList)
            } catch (e : Exception) {
                Log.d("NewsRepository", "getHeadlineNews: ${e.message.toString()}")
                emit(Result.Error(e.message.toString()))
        }
        val localData: LiveData<Result<List<NewsEntity>>> = newsDao.getNews().map { Result.Success(it) }
        emitSource(localData)

//        result.value = Result.Loading
//        val client = apiService.getNews(BuildConfig.API_KEY)
//        client.enqueue(object : Callback<NewsResponse> {
//            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
//                if (response.isSuccessful) {
//                    val articles = response.body()?.articles
//                    val newsList = ArrayList<NewsEntity>()
//                    appExecutors.diskIO.execute {
//                        articles?.forEach { articles ->
//                            val isBookmarked = newsDao.isNewsBookmarked(articles.title)
//                            val news = NewsEntity(
//                                articles.title,
//                                articles.publishedAt,
//                                articles.urlToImage,
//                                articles.url,
//                                isBookmarked
//                            )
//                            newsList.add(news)
//                        }
//                        newsDao.deleteAll()
//                        newsDao.insertNews(newsList)
//                    }
//                }
//            }
//
//            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
//                result.value = Result.Error(t.message.toString())
//            }
//        })
//        val localData = newsDao.getNews()
//        result.addSource(localData) { newData: List<NewsEntity> ->
//            result.value = Result.Success(newData)
//        }
//        return result
//    }
//
//    fun getBookmarkedNews(): LiveData<List<NewsEntity>> {
//        return newsDao.getBookmarkedNews()
//    }
//    fun setBookmarkedNews(news: NewsEntity, bookmarkState: Boolean) {
//        appExecutors.diskIO.execute {
//            news.isBookmarked = bookmarkState
//            newsDao.updateNews(news)
//        }
//    }
//
//    companion object {
//        @Volatile
//        private var instance: NewsRepository? = null
//        fun getInstance (
//            apiService: ApiService,
//            newsDao: NewsDao,
//            appExecutors: AppExecutors
//        ) : NewsRepository =
//            instance ?: synchronized(this) {
//                instance ?: NewsRepository(apiService, newsDao, appExecutors)
//            }.also { instance = it }
    }
    fun getBookmarkedNews(): LiveData<List<NewsEntity>> {
        return newsDao.getBookmarkedNews()
    }
    suspend fun setNewsBookmark(news: NewsEntity, bookmarkState: Boolean) {
        news.isBookmarked = bookmarkState
        newsDao.updateNews(news)
    }
    companion object {
        @Volatile
        private var instance: NewsRepository? = null
        fun getInstance(
            apiService: ApiService,
            newsDao: NewsDao,
            appExecutors: AppExecutors
        ): NewsRepository =
            instance ?: synchronized(this) {
                instance ?: NewsRepository(apiService, newsDao, appExecutors)
            }.also { instance = it }
    }
}
package tech.medivh.plugin.gradle.publisher.api

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Response


/**
 * @author gxz gongxuanzhangmelt@gmail.com
 **/
fun interface SonatypeResponseHandler<T> {

    fun handleResponse(response: Response): T

}


abstract class DefaultSonatypeResponseHandler<T> : SonatypeResponseHandler<T> {

    internal val seeDoc = "see https://central.sonatype.com/api-doc"

    var codeHandleMap = mutableMapOf<Int, (Response) -> T>().apply {
        put(400) {
            throw IllegalStateException("Wrong authorization data (user/password or token). ${errorMsgTail(it)}")
        }
        put(401) {
            throw IllegalStateException("The user does not have an active session or is not authenticated. ${errorMsgTail(it)}")
        }
        put(403) {
            throw IllegalStateException("The user is not authorized to perform this action.. ${errorMsgTail(it)}")
        }
        put(500) {
            throw IllegalStateException("Internal server error. ${errorMsgTail(it)}")
        }
    }

    private fun errorMsgTail(response: Response): String {
        return " $seeDoc\nrequest:[${response.request.url}]"
    }

    override fun handleResponse(response: Response): T {
        if (response.isSuccessful) {
            return handleSuccess(response)
        }
        return codeHandleMap[response.code]?.invoke(response)
            ?: throw IllegalStateException("Unknown error code ${response.code}")
    }

    abstract fun handleSuccess(response: Response): T

}


object UploadResponseHandler : DefaultSonatypeResponseHandler<String>() {

    override fun handleSuccess(response: Response): String {
        return response.body!!.string()
    }

}

object StatusResponseHandler : DefaultSonatypeResponseHandler<DeploymentState>() {

    init {
        codeHandleMap[404] = {
            throw IllegalStateException("Deployment with provided id not found. $seeDoc")
        }
    }

    override fun handleSuccess(response: Response): DeploymentState {
        val objectMapper = ObjectMapper()
        objectMapper.readTree(response.body!!.string()).let {
            return DeploymentState.valueOf(it["deploymentState"].asText())
        }
    }

}

object DropResponseHandler : DefaultSonatypeResponseHandler<Unit>() {

    init {
        StatusResponseHandler.codeHandleMap[404] = {
            throw IllegalStateException("Deployment not found.. ${StatusResponseHandler.seeDoc}")
        }
    }

    override fun handleSuccess(response: Response) {
        //  drop success not need to handle
    }

}

object PublishResponseHandler : DefaultSonatypeResponseHandler<Unit>() {

    init {
        StatusResponseHandler.codeHandleMap[404] = {
            throw IllegalStateException("Deployment not found.. ${StatusResponseHandler.seeDoc}")
        }
    }

    override fun handleSuccess(response: Response) {
        //  publish success not need to handle
    }

}

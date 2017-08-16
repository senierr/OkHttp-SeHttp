package com.senierr.sehttp.emitter;

import com.senierr.sehttp.SeHttp;
import com.senierr.sehttp.callback.BaseCallback;
import com.senierr.sehttp.request.RequestBuilder;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 请求发射器
 *
 * 用于发送执行，取消请求，缓存及事件分发等
 *
 * @author zhouchunjie
 * @date 2017/3/29
 */

public class Emitter<T> {

    // 请求实体
    private RequestBuilder requestBuilder;
    // 回调
    private BaseCallback<T> callback;

    public Emitter(RequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }

    /**
     * 异步执行
     *
     * @param baseCallback
     */
    public void execute(BaseCallback<T> baseCallback) {
        this.callback = baseCallback;
        // Before回调
        if (callback != null) {
            SeHttp.getInstance().getMainScheduler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onBefore(requestBuilder);
                }
            });
        }
        // 构建请求
        final Request request = requestBuilder.build(callback);
        // 网络请求
        getNewCall(request).enqueue(new Callback() {
            int currentRetryCount = 0;

            @Override
            public void onFailure(Call call, final IOException e) {
                if (!call.isCanceled() && currentRetryCount < SeHttp.getInstance().getRetryCount()) {
                    currentRetryCount++;
                    getNewCall(request).enqueue(this);
                } else {
                    sendError(call, e);
                }
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (callback != null) {
                    try {
                        T t = callback.convert(response);
                        sendSuccess(call, t, false);
                    } catch (Exception e) {
                        sendError(call, e);
                    }
                }
                response.close();
            }
        });
    }

    /**
     * 同步执行
     *
     * @return
     */
    public Response execute() throws IOException {
        return getNewCall(requestBuilder.build(callback)).execute();
    }

    /**
     * 创建Call对象
     *
     * @return
     */
    private Call getNewCall(Request request) {
        return SeHttp.getInstance().getOkHttpClient().newCall(request);
    }

    /**
     * 执行成功回调
     *
     * @param t
     */
    private void sendSuccess(final Call call, final T t, final boolean isCache) {
        if (callback == null) {
            return;
        }
        SeHttp.getInstance().getMainScheduler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onSuccess(t, isCache);
                    callback.onAfter();
                } catch (Exception e) {
                    sendError(call, e);
                }
            }
        });
    }

    /**
     * 执行失败回调
     *
     * @param e
     */
    private void sendError(final Call call, final Exception e) {
        if (callback == null) {
            return;
        }
        if (call == null || !call.isCanceled()) {
            SeHttp.getInstance().getMainScheduler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(e);
                    callback.onAfter();
                }
            });
        }
    }
}

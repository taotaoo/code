// gcc -o tcpserver server.c -I deps/libuv/include/ -I deps/cjson/include -L deps/libuv/lib/ deps/cjson/lib/cJSON.o -luv -lm -lpthread -g

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "cJSON.h"
#include "uv.h"

typedef struct
{
    char data[20480];
    int len;
} tcp_buf_t;

#define TLOGF(func, tag, fmt, args...) printf("fatal: "tag" ["func"] "fmt"\n", ##args)
#define TLOGE(func, tag, fmt, args...) printf("error: "tag" ["func"] "fmt"\n", ##args)
#define TLOGW(func, tag, fmt, args...) printf("warn: "tag" ["func"] "fmt"\n", ##args)
#define TLOGI(func, tag, fmt, args...) printf("info: "tag" ["func"] "fmt"\n", ##args)
#define TLOGD(func, tag, fmt, args...) printf("debug: "tag" ["func"] "fmt"\n", ##args)
#define CHECK(exp, obj) \
    do { \
        if (!(exp)) { \
            int *err = __errno_location(); \
            if (err != NULL){ \
                TLOGE("", "", "assert '%s' and '%s', error %s \n", (#exp), (#obj), strerror(*err)); \
            } \
            else { \
                TLOGE("", "", "assert '%s' and '%s', error unkown \n", (#exp), (#obj)); \
            } \
            abort(); \
        } \
    } while (0)

#define PORT 12345
uv_tcp_t tcp_handle;

static void tcp_close_cb(uv_handle_t* handle)
{
    free(handle);
}

static void tcp_write_cb(uv_write_t *req, int status)
{
    TLOGD("tcp_write_cb", "", "send echo response success.");
    free(req->data);
    req->data = NULL;
    free(req);
    req = NULL;
}

static void process(uv_stream_t *handle, cJSON *req)
{
    char *json = cJSON_Print(req);
    TLOGD("process", "", "process json. %s", json);
    // echo response
    uv_buf_t buf = uv_buf_init(json, strlen(json));
    uv_write_t *write_req = malloc(sizeof(*write_req)); // 在write_cb中释放
    CHECK(write_req != NULL , "malloc(uv_write_t) failed.");
    write_req->data = json;
    // 发送上线消息
    if (uv_write(write_req, handle, &buf, 1, tcp_write_cb) < 0)
    {
        TLOGE("login", "", "uv_write error. clustering failed");
        uv_close((uv_handle_t *)handle, tcp_close_cb);
        return;
    }
}

const char *json_decode(const cJSON **out, const char *value)
{
    const char *end;
    *out = cJSON_ParseWithOpts(value, &end, 0);
    if (*out == NULL)
    {
        if (cJSON_GetErrorPtr() == (value + strlen(value)))
        {
            return cJSON_GetErrorPtr();
        }
        else
        {
            return NULL;
        }
    }
    else
    {
        return end;
    }
}

static void tcp_read_cb(uv_stream_t *handle, ssize_t nread, const uv_buf_t *buf)
{
    if (nread < 0)
    {
        TLOGI("tcp_read_cb", "", "connection disconnect...%d", UV_EOF);
        free(handle->data);
        uv_close((uv_handle_t *)handle, tcp_close_cb);
        return;
    }
    if (nread == 0)
    {   // 没有读到数据，本次直接返回
        return;
    }

    tcp_buf_t *tcp_buf = handle->data;
    tcp_buf->len += nread;
    cJSON *req;
    const char *end;
    // 处理 channel_adapter 发来的消息
    while (tcp_buf->len > 0)
    {
        tcp_buf->data[tcp_buf->len] = '\0';
        TLOGD("tcp_read_cb", "", "tcp_read_cb: %s", tcp_buf->data);
        req = NULL;
        end = json_decode((const cJSON **)&req, tcp_buf->data);
        if (end == NULL)
        {
            TLOGE("tcp_read_cb", "", "json_decode error");
            tcp_buf->len = 0;
            break;
        }
        if (req == NULL)
        {
            break;
        }
        tcp_buf->len = tcp_buf->data + tcp_buf->len - end;
        if (tcp_buf->len > 0)
        {
            memmove(tcp_buf->data, end, tcp_buf->len);
        }
        process(handle, req);
        cJSON_Delete(req);
    }
}

static void tcp_alloc_cb(uv_handle_t *handle, size_t buf_size, uv_buf_t *buf)// TODO 防止粘包
{
    tcp_buf_t *tcp_buf = handle->data;
    if (tcp_buf->len >= (sizeof(tcp_buf->data) - 1))
    {
        TLOGE("tcp_alloc_cb", "", "tcp_alloc_cb error.");
        tcp_buf->len = 0;
    }
    buf->base = tcp_buf->data + tcp_buf->len;
    buf->len = sizeof(tcp_buf->data) - tcp_buf->len - 1;
}

static void listen_cb(uv_stream_t* server, int status)
{
    if (status < 0)
    {
        TLOGE("listen_cb", "", "listen failed");
        return;
    }
    uv_tcp_t* client = malloc(sizeof(*client));
    CHECK(client != NULL, "uv_tcp_t malloc failed.");
    int ret = 0;
    ret = uv_tcp_init(uv_default_loop(), client);
    if (ret < 0)
    {
        TLOGE("listen_cb", "", "init error: %s", uv_strerror(ret));
        return;
    }
    ret = uv_accept(server, (uv_stream_t*)client);
    if (ret < 0)
    {
        TLOGE("listen_cb", "", "accept error: %s", uv_strerror(ret));
        free(client);
        return;
    }
    TLOGD("listen_cb", "", "uv_accept(%p)", client);

    tcp_buf_t *buf = (tcp_buf_t *)malloc(sizeof(*buf));
    CHECK(buf != NULL, "tcp_buf_t malloc failed.");
    bzero(buf, sizeof(*buf));
    client->data = buf;

    // 开始接收客户端发来的数据
    uv_read_start((uv_stream_t*)client, tcp_alloc_cb, tcp_read_cb);
}

void main()
{
    int ret;
    struct sockaddr_in sockaddr;
    ret = uv_ip4_addr("0.0.0.0", PORT, &sockaddr);
    CHECK(ret == 0 , "uv_ip4_addr error.");
    // 初始化内网卡 tcp server
    ret = uv_tcp_init(uv_default_loop(), &tcp_handle);
    CHECK(ret == 0, "uv_tcp_init error");
    ret = uv_tcp_bind(&tcp_handle, (const struct sockaddr *)&sockaddr, 0);
    CHECK(ret == 0, "uv_tcp_bind error");
    ret = uv_listen((uv_stream_t *)&tcp_handle, SOMAXCONN, listen_cb);
    CHECK(ret == 0, "uv_listen error");
    uv_run(uv_default_loop(), UV_RUN_DEFAULT);
    return;
}

### Akka http

- Streaming:
  - Streaming responses will be `backpressured` by `the remote clients` so that the server will not push data faster than the client can handle, streaming requests means that the server decides how fast the more clients can push the data of the request body.
- URI parsing mode:
  - [`def strict(queryString: String): Query = Query(queryString, mode = Uri.ParsingMode.Strict)`](https://doc.akka.io/docs/akka-http/current/common/uri-model.html#query-string-in-uri)
    - Strict mode is a strict parsing mode: i.d. Doesn't allow double '=', or single ';', '^' in query string.
  - [`def relaxed(queryString: String): Query = Query(queryString, mode = Uri.ParsingMode.Relaxed)`](https://doc.akka.io/docs/akka-http/current/common/uri-model.html#strict-and-relaxed-mode)
    - Relaxed mode allows cases shown above.

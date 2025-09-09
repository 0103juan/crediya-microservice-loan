package co.com.pragma.api.mapper;

import co.com.pragma.model.pagequery.PageQuery;
import org.springframework.web.reactive.function.server.ServerRequest;

public class PageQueryMapper {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    public static PageQuery from(ServerRequest request) {
        int page = request.queryParam("page")
                .map(Integer::parseInt)
                .orElse(DEFAULT_PAGE);

        int size = request.queryParam("size")
                .map(Integer::parseInt)
                .orElse(DEFAULT_SIZE);

        return new PageQuery(page, size);
    }
}

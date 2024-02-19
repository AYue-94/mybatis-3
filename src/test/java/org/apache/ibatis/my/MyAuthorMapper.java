package org.apache.ibatis.my;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

public interface MyAuthorMapper {

  @Select("select * from author where id = #{id}")
  Author selectAuthorByAnnotation(@Param("id") int id);

  Author selectAuthorClassic(@Param("id") int id);

  void insert(Author author);

  List<Author> selectByCondition(Author author);

  @Select("select * from author where id = #{param1} and username = #{username}")
  Author selectAuthorByIdAndName(Integer id, @Param("username") String name);

  @Select("<script>select *\n" +
    "        from author\n" +
    "        where id in\n" +
    "        <foreach collection=\"collection\" item=\"i\" open=\"(\" close=\")\" separator=\",\">\n" +
    "            #{i}\n" +
    "        </foreach></script>")
  List<Author> selectByIds(List<Integer> ids);

  // 流式查询：请求mysql，mysql返回全量数据，但是ResultSet每读一行，只从socket InputStream中读一行到堆内存
//  @Options(fetchSize = Integer.MIN_VALUE)
  // 游标查询：配合useCursorFetch=true使用，每次请求mysql返回3条数据，ResultSet遍历完3条后，再次请求mysql返回3条
//  @Options(fetchSize = 3)

  @Select("select * from author")
  @Options(fetchSize = 10)
  Cursor<Author> selectCursor();

  List<Author> selectRowBounds(RowBounds rowBounds);

  void updateOne(Author author);

  @Delete("delete from author")
  void deleteAll();

  @Select("<script>select *\n" +
    "        from author\n" +
    "        where id in\n" +
    "        <foreach collection=\"ids\" item=\"i\" open=\"(\" close=\")\" separator=\",\">\n" +
    "            #{i}\n" +
    "        </foreach></script>")
  @ResultType(Author.class)
  void selectMap(@Param("ids") List<Integer> ids, ResultHandler<Author> resultHandler);
}

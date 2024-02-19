package org.apache.ibatis.my;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MyTest extends BaseDataTest {
  private static SqlSessionFactory sqlSessionFactory;

  @BeforeAll
  static void setup() throws Exception {
    DataSource dataSource = createUnpooledDataSource("org/apache/ibatis/my/datasource.properties");

//     case1 使用xml配置Configuration
//    final String resource = "org/apache/ibatis/builder/MapperConfig.xml";
//    final Reader reader = Resources.getResourceAsReader(resource);
//    sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);

    // case2 编码配置Configuration
    TransactionFactory transactionFactory = new JdbcTransactionFactory();
    // 数据源 + 事务管理器 -> Environment
    Environment environment = new Environment("development", transactionFactory, dataSource);
    // Environment + Mapper -> Configuration
    Configuration configuration = new Configuration(environment);
    configuration.addMapper(MyAuthorMapper.class);

    configuration.addInterceptor(new MyPlugin());
    // Configuration -> SqlSessionFactory
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
  }

  @Test
  void test() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {

      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);

      // 1. 通过@Select注解配置sql
      System.out.println(mapper.selectAuthorByAnnotation(101));

      // 2. 通过xml配置sql
      System.out.println(mapper.selectAuthorClassic(101));

      // 3. 使用语句id执行sql
      Author author =
        sqlSession.selectOne("org.apache.ibatis.my.MyAuthorMapper.selectAuthorClassic", 101);
      System.out.println(author);
    }
  }

  @Test
  void test2() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {

      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);

      Author author = mapper.selectAuthorByIdAndName(101, "aaa");

      System.out.println(author);

      List<Author> authors = mapper.selectByIds(Arrays.asList(101, 102));

      System.out.println(authors);
    }
  }

  @Test
  void testDynamic() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {

      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);

      List<Author> author = mapper.selectByIds(Arrays.asList(101, 102));

      System.out.println(author);
    }
  }

  @Test
  void testDynamic2() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {

      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);

      Author author = new Author();
      author.setId(101);
      author.setUsername("jim");
      List<Author> authors = mapper.selectByCondition(author);

      System.out.println(authors);
    }
  }

  @Test
  void testRaw() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {

      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);

      Author author = mapper.selectAuthorClassic(101);

      System.out.println(author);
    }
  }

  @Test
  void testInsert() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
      Author author = new Author();
      author.setUsername("test");
      author.setPassword("test");
      author.setEmail("aaa");
      author.setBio("aaa");
      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);
      mapper.insert(author);

      System.out.println(author.getId());
    }
  }

  @Test
  void testResult1() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Author author =
        sqlSession.selectOne("org.apache.ibatis.my.MyAuthorMapper.selectReturnResultMap", 101);
      System.out.println(author);
    }
  }

  @Test
  void testResult2() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      List<Author> author =
        sqlSession.selectList("org.apache.ibatis.my.MyAuthorMapper.selectReturnResultType", 101);
      System.out.println(author);
    }
  }

  @Test
  void testUpdate() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      Author author = new Author();
      author.setId(101);
      author.setUsername("xxx");
      int count =
        sqlSession.update("org.apache.ibatis.my.MyAuthorMapper.updateOne", author);
      System.out.println(count);
    }
  }

  @Test
  void testRowBounds() throws IOException, InterruptedException {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);
      List<Author> authors = mapper.selectRowBounds(new RowBounds(2, 5));
      System.out.println(authors);
    }
  }

  /**
   * sudo tcpdump -X -i any -s 0 -tttt dst port 3306
   */
  @Test
  void testCursor() throws IOException, InterruptedException {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);
      try(Cursor<Author> authors = mapper.selectCursor()) {
        for (Author author : authors) {
          System.out.println(author);
        }
      }
    }
  }

  @Test
  void testBatch() {
    deleteAll();
    try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);
      for (int i = 1; i <= 10; i++) {
        Author author = new Author(i, "username" + i, "password" + i, "email" + i, "aaa", null);
        mapper.insert(author);
      }
      for (int i = 1; i <= 10; i++) {
        Author author = new Author(i);
        author.setUsername("xxx");
        mapper.updateOne(author);
      }
      sqlSession.commit();
    }
  }

  @Test
  void testBatch2() {
    deleteAll();
    try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);
      for (int i = 1; i <= 10; i++) {
        Author author = new Author(i, "username" + i, "password" + i, "email" + i, "aaa", null);
        mapper.insert(author);
        author = new Author(i);
        author.setUsername("xxx");
        mapper.updateOne(author); // sql切换，批处理失效
      }
      sqlSession.commit();
    }
  }

  @Test
  void testDelete() {
    deleteAll();
  }

  @Test
  void testJdbcBatch() throws SQLException {
    deleteAll();
    Connection connection = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource().getConnection();
    connection.setAutoCommit(false);

    PreparedStatement statement = connection.prepareStatement("insert into author (id,username) values (?,?)");
    for (int i = 1; i <= 10; i++) {
      statement.setInt(1, i);
      statement.setString(2, "username" + i);
      statement.addBatch();
    }

//    PreparedStatement preparedStatement = connection.prepareStatement("select * from author where id = 1");
//    ResultSet set = preparedStatement.executeQuery();
//    System.out.println("set.next ? " + set.next()); // set.next ? false
//    while (set.next()) {
//      System.out.println(set.getString("username"));
//    }
//    set.close();

    PreparedStatement statement2 = connection.prepareStatement("update author set username = ? where id = ?");
    for (int i = 1; i <= 10; i++) {
      statement2.setString(1, "xxx");
      statement2.setInt(2, i);
      statement2.addBatch();
    }

    int[] i = statement.executeBatch();
    for (int j = 0; j < i.length; j++) {
      System.out.println("count1 = " + i[j]);
    }
    int[] i1 = statement2.executeBatch();
    for (int j = 0; j < i1.length; j++) {
      System.out.println("count2 = " + i1[j]);
    }

    connection.commit();
  }

  @Test
  void testJdbcBatch1() throws SQLException {
    deleteAll();
    Connection connection = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource().getConnection();
    connection.setAutoCommit(false);
    PreparedStatement statement = connection.prepareStatement("insert into author (id,username) values (?,?)");
    for (int i = 1; i <= 10; i++) {
      statement.setInt(1, i);
      statement.setString(2, "username" + i);
      statement.addBatch();
    }
    int[] i = statement.executeBatch();
    System.out.println("count1 = " + i.length);
    connection.commit();
  }

  @Test
  void testReuse() {
    deleteAll();
    try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.REUSE, false)) {
      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);
      for (int i = 1; i <= 3; i++) {
        Author author = new Author(i, "username" + i, "password" + i, "email" + i, "aaa", null);
        mapper.insert(author);
      }
      sqlSession.commit();
    }
  }

  @Test
  void testResultHandler() {
    AuthorNameHandler handler = new AuthorNameHandler();
    try (SqlSession session = sqlSessionFactory.openSession()) {
      MyAuthorMapper mapper = session.getMapper(MyAuthorMapper.class);
      mapper.selectMap(Arrays.asList(1,2,3), handler);
    }
    System.out.println(handler.getName(1));
  }

  private static class AuthorNameHandler implements ResultHandler<Author> {
    final Map<Integer, String> id2Name = new HashMap<>();
    @Override
    public void handleResult(ResultContext<? extends Author> resultContext) {
      Author value = resultContext.getResultObject();
      id2Name.put(value.getId(), value.getUsername());
    }
    public String getName(Integer id) {
      return id2Name.get(id);
    }
  }

  private void deleteAll() {
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
      MyAuthorMapper mapper = sqlSession.getMapper(MyAuthorMapper.class);
      mapper.deleteAll();
      sqlSession.commit();
    }
  }

  @Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
  })
  private static class MyPlugin implements Interceptor {
    private int intercept_count = 0;
    private int plugin_count = 0;
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
      System.out.println("intercept count = " + ++intercept_count);
      return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
      System.out.println("plugin count = " + ++plugin_count);
      return Interceptor.super.plugin(target);
    }
  }
}

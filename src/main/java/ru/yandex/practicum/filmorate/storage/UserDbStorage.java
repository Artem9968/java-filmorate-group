package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exceptions.DataNotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
@Primary
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {
    private final NamedParameterJdbcOperations jdbc;

    private User mapRowToUser(ResultSet resultSet, int rowNum) throws SQLException {
        return User.builder()
                .id(resultSet.getLong("user_id"))
                .email(resultSet.getString("email"))
                .login(resultSet.getString("login"))
                .name(resultSet.getString("name"))
                .birthday(LocalDate.parse(resultSet.getString("birthday")))
                .build();
    }

    @Override
    public User create(User user) {
        String sqlQuery = "INSERT INTO users (email, login, name, birthday) " +
                "VALUES (:email, :login, :name, :birthday);";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sqlQuery, new MapSqlParameterSource(user.toMap()), keyHolder);
        user.setId(keyHolder.getKeyAs(Long.class));
        return user;
    }

    @Override
    public User getUserById(Long userId) {
        String sql = "SELECT * FROM users WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        List<User> users = jdbc.query(sql, params, this::mapRowToUser);
        if (users.isEmpty()) {
            throw new DataNotFoundException("Пользователь с id=" + userId + " не найден");
        }
        return users.get(0);
    }

    @Override
    public Collection<User> findAll() {
        String sqlQuery = "SELECT * FROM users;";
        return jdbc.query(sqlQuery, this::mapRowToUser);
    }

    @Override
    public User update(User user) {
        String sqlQuery = "UPDATE users SET email = :email, login = :login, name = :name," +
                " birthday = :birthday WHERE user_id = :user_id;";
        jdbc.update(sqlQuery, user.toMap());
        return user;
    }

    @Override
    public void deleteUserById(Long userId) {
        String sqlQuery = "DELETE FROM users WHERE user_id = :user_id;";
        jdbc.update(sqlQuery, new MapSqlParameterSource("user_id", userId));
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        String sqlQuery = "INSERT INTO friendship (user_id, friend_id) VALUES (:user_id, :friend_id);";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("user_id", userId);
        params.addValue("friend_id", friendId);

        jdbc.update(sqlQuery, params);
    }

    @Override
    public Collection<User> getFriendsByUser(Long userId) {
        String sqlQuery = "SELECT * FROM users WHERE user_id IN " +
                "(SELECT friend_id FROM friendship WHERE user_id = :user_id);";

        return jdbc.query(sqlQuery, new MapSqlParameterSource("user_id", userId), this::mapRowToUser);
    }

    @Override
    public Collection<User> getCommonFriends(Long firstUserId, Long secondUserId) {
        String sqlQuery = "SELECT * FROM users WHERE user_id IN " +
                "(SELECT friend_id FROM friendship WHERE user_id = :first_user_id " +
                "INTERSECT " +
                "SELECT friend_id FROM friendship WHERE user_id = :second_user_id);";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("first_user_id", firstUserId);
        params.addValue("second_user_id", secondUserId);
        return jdbc.query(sqlQuery, params, this::mapRowToUser);
    }

    @Override
    public void deleteFriend(Long userId, Long friendId) {
        String sqlQuery = "DELETE FROM friendship WHERE user_id = :user_id AND friend_id = :friend_id;";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("user_id", userId);
        params.addValue("friend_id", friendId);

        jdbc.update(sqlQuery, params);
    }

    @Override
    public boolean isUserExistsWithEmail(User user) {
        String sqlQuery = "SELECT COUNT(*) FROM users WHERE email = :email" +
                (user.getId() == null ? "" : " AND user_id <> :user_id") + ";";
        return 1 == jdbc.queryForObject(sqlQuery, new MapSqlParameterSource(user.toMap()), Integer.class);
    }

    @Override
    public boolean isUserExists(Long userId) {
        String sqlQuery = "SELECT COUNT(*) FROM users WHERE user_id = :user_id;";

        return 1 == jdbc.queryForObject(sqlQuery, new MapSqlParameterSource("user_id", userId), Integer.class);
    }
}
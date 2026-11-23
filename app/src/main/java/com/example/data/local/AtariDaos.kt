package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CheatEntity
import com.example.data.model.DownloadTaskEntity
import com.example.data.model.GameEntity
import com.example.data.model.SaveStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT COUNT(*) FROM games")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getCount(): Int

    @Query("SELECT * FROM games ORDER BY lastPlayedAt DESC LIMIT 1")
    fun observeLastPlayed(): Flow<GameEntity?>

    @Query("SELECT * FROM games ORDER BY addedAt DESC LIMIT 15")
    fun observeRecentlyAdded(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games ORDER BY playTimeSeconds DESC, title ASC LIMIT 15")
    fun observeMostPlayed(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isFavorite = 1 ORDER BY playTimeSeconds DESC, title ASC")
    fun observeFavorites(): Flow<List<GameEntity>>

    @Query("SELECT DISTINCT genre FROM games ORDER BY genre ASC")
    fun observeGenres(): Flow<List<String>>

    @Query("SELECT DISTINCT year FROM games ORDER BY year DESC")
    fun observeYears(): Flow<List<Int>>

    @Query(
        """
        SELECT * FROM games
        WHERE (:system = '' OR system = :system)
          AND (:genre = '' OR genre = :genre)
          AND (:year = 0 OR year = :year)
          AND (:query = '' OR title LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%')
        ORDER BY
          CASE WHEN :sortMode = 'MOST_PLAYED' THEN playTimeSeconds END DESC,
          CASE WHEN :sortMode = 'NEWEST' THEN year END DESC,
          CASE WHEN :sortMode = 'NEWEST' THEN addedAt END DESC,
          title ASC
        LIMIT 3000
        """
    )
    fun observeFilteredGames(
        system: String,
        genre: String,
        year: Int,
        query: String,
        sortMode: String
    ): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GameEntity?

    @Query("SELECT * FROM games WHERE title LIKE '%' || :query || '%' ORDER BY playTimeSeconds DESC LIMIT 20")
    suspend fun quickSearch(query: String): List<GameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameEntity)

    @Query("UPDATE games SET isFavorite = NOT isFavorite WHERE id = :gameId")
    suspend fun toggleFavorite(gameId: String)

    @Query("UPDATE games SET lastPlayedAt = :timestamp, playTimeSeconds = playTimeSeconds + :deltaSeconds WHERE id = :gameId")
    suspend fun recordPlaySession(gameId: String, timestamp: Long, deltaSeconds: Long)

    @Query("UPDATE games SET coverPath = :coverPath WHERE id = :gameId")
    suspend fun updateCoverPath(gameId: String, coverPath: String)

    @Query("SELECT * FROM games")
    suspend fun getAllSnapshot(): List<GameEntity>
}

@Dao
interface SaveStateDao {
    @Query("SELECT * FROM save_states WHERE gameId = :gameId ORDER BY slotIndex ASC")
    fun observeForGame(gameId: String): Flow<List<SaveStateEntity>>

    @Query("SELECT * FROM save_states WHERE gameId = :gameId AND slotIndex = :slotIndex LIMIT 1")
    suspend fun getSlot(gameId: String, slotIndex: Int): SaveStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(saveState: SaveStateEntity)

    @Query("DELETE FROM save_states WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM save_states")
    suspend fun getAllSnapshot(): List<SaveStateEntity>
}

@Dao
interface CheatDao {
    @Query("SELECT * FROM cheats WHERE gameId = :gameId ORDER BY name ASC")
    fun observeForGame(gameId: String): Flow<List<CheatEntity>>

    @Query("SELECT * FROM cheats WHERE gameId = :gameId AND enabled = 1")
    suspend fun getActiveForGame(gameId: String): List<CheatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cheat: CheatEntity)

    @Query("UPDATE cheats SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM cheats WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM cheats")
    suspend fun getAllSnapshot(): List<CheatEntity>
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: DownloadTaskEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)
}

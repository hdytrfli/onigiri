package dev.hdytrfli.onigiri.database

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.DeleteTable
import androidx.room3.RoomDatabase
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import dev.hdytrfli.onigiri.models.FindroidBookDto
import dev.hdytrfli.onigiri.models.FindroidEpisodeDto
import dev.hdytrfli.onigiri.models.FindroidMediaStreamDto
import dev.hdytrfli.onigiri.models.FindroidMovieDto
import dev.hdytrfli.onigiri.models.FindroidSeasonDto
import dev.hdytrfli.onigiri.models.FindroidSegmentDto
import dev.hdytrfli.onigiri.models.FindroidShowDto
import dev.hdytrfli.onigiri.models.FindroidSourceDto
import dev.hdytrfli.onigiri.models.FindroidTrickplayInfoDto
import dev.hdytrfli.onigiri.models.FindroidUserDataDto
import dev.hdytrfli.onigiri.models.Server
import dev.hdytrfli.onigiri.models.ServerAddress
import dev.hdytrfli.onigiri.models.User

@Database(
    entities =
        [
            Server::class,
            ServerAddress::class,
            User::class,
            FindroidMovieDto::class,
            FindroidBookDto::class,
            FindroidShowDto::class,
            FindroidSeasonDto::class,
            FindroidEpisodeDto::class,
            FindroidSourceDto::class,
            FindroidMediaStreamDto::class,
            FindroidUserDataDto::class,
            FindroidTrickplayInfoDto::class,
            FindroidSegmentDto::class,
            QueryCacheEntity::class,
        ],
    version = 10,
    autoMigrations =
        [
            AutoMigration(from = 2, to = 3),
            AutoMigration(from = 3, to = 4),
            AutoMigration(from = 4, to = 5, spec = ServerDatabase.TrickplayMigration::class),
            AutoMigration(from = 5, to = 6, spec = ServerDatabase.IntrosMigration::class),
            AutoMigration(from = 7, to = 8),
            AutoMigration(from = 8, to = 9),
            AutoMigration(from = 9, to = 10),
        ],
)
@ColumnTypeConverters(Converters::class)
abstract class ServerDatabase : RoomDatabase() {
    abstract fun getServerDatabaseDao(): ServerDatabaseDao

    @DeleteTable(tableName = "trickPlayManifests") class TrickplayMigration : AutoMigrationSpec

    @DeleteTable(tableName = "intros") class IntrosMigration : AutoMigrationSpec
}

val MIGRATION_6_7 =
    object : Migration(startVersion = 6, endVersion = 7) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("DROP TABLE segments")
            connection.execSQL(
                "CREATE TABLE segments (`itemId` TEXT NOT NULL, `type` TEXT NOT NULL, `startTicks` INTEGER NOT NULL, `endTicks` INTEGER NOT NULL, PRIMARY KEY(`itemId`, `type`), FOREIGN KEY(`itemId`) REFERENCES `episodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
        }
    }

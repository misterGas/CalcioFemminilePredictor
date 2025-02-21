package com.embeddedproject.calciofemminileitaliano.helpers

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserLoggedInHelper(context: Context): SQLiteOpenHelper(context, "UserAndTeamsImages", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        var sql = "CREATE TABLE USER (UserNickname text primary key)"
        db?.execSQL(sql)
        sql = "CREATE TABLE TEAM_IMAGE (TeamName text primary key, ImageBitmap blob not null)"
        db?.execSQL(sql)
        sql = "CREATE TABLE USER_LAST_ACCESSED (UserNickname text, Championship text, Season text, LastRound integer not null, LastMatchInRound integer not null, primary key(UserNickname, Championship, Season))"
        db?.execSQL(sql)
        sql = "CREATE TABLE LAST_UPDATED_TIME (UserNickname text primary key, UpdatedTime text not null)"
        db?.execSQL(sql)
        sql = "CREATE TABLE LAST_UPDATED_NICKNAME (UserNickname text primary key, UpdatedTime text not null)"
        db?.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}
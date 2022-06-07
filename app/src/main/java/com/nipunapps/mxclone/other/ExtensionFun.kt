package com.nipunapps.mxclone.other

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import java.util.*
import kotlin.math.roundToInt

fun Context.showShortToast(message : String){
    Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
}

fun Context.showShortToast(@StringRes resId : Int,vararg extra : Any){
    Toast.makeText(this,getString(resId,extra),Toast.LENGTH_SHORT).show()
}

fun Context.showLongToast(message : String){
    Toast.makeText(this,message,Toast.LENGTH_LONG).show()
}

fun Context.showLongToast(@StringRes resId : Int, vararg extra : Any){
    Toast.makeText(this,getString(resId,extra),Toast.LENGTH_LONG).show()
}

fun Long.getDurationInFormat(): String {
    var second = this / 1000
    var minute = second / 60
    second %= 60
    if (minute >= 60) {
        val hour = minute / 60
        minute %= 60
        return "${hour.getWithZero()}:${minute.getWithZero()}:${second.getWithZero()}"
    }
    return "${minute.getWithZero()}:${second.getWithZero()}"
}

fun Long.getWithZero(): String {
    return if (this < 10) "0$this" else this.toString()
}

fun Long.getSizeInHigherByte() : String{
    val sizeInDouble = this.toDouble()
    val kb = sizeInDouble/1024
    if(kb >= 1024){
        val mb = kb/1024
        if(mb >= 1024){
            val gb = mb/1024
            return gb.roundTo2Decimal().toString()+"GB"
        }
        return mb.roundTo2Decimal().toString()+"MB"
    }
    return kb.roundTo2Decimal().toString()+"KB"
}

fun Double.roundTo2Decimal() : Double{
    return (this*100.0).roundToInt() / 100.0
}

fun Long.getTimeInDate(): String {
    val date = Date(this * 1000L)
    return date.toString().substring(11, 16)+", "+date.toString().substring(0, 3) + ", " + date.toString().substring(4, 10) + ", " +
            date.toString().takeLastWhile { !it.isWhitespace() }
}
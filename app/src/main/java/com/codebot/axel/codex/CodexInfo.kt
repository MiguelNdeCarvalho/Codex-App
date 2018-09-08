package com.codebot.axel.codex

/**
 * Created by Axel on 6/9/2018.
 */

// CodeX-Kernel data
class CodexInfo(val name: String, val description: String, val features: ArrayList<String>, val governors: ArrayList<String>, val schedulers: ArrayList<String>, val downloads: Downloads, val changelog: Array<Changelog>)

class Downloads(val name: String, val ver: String, var url: String)
class Changelog(val added: Array<String>)
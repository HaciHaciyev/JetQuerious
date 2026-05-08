package io.github.hacihaciyev.sql.internal

import io.github.hacihaciyev.sql.JQ

object TestFixtures {
    private object StubDQL extends DQL
    val stubJqRead: JQ.Read = new JQ.Read("SELECT 1", StubDQL)
}
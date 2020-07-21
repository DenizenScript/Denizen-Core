package com.denizenscript.denizencore.utilities.data;

public enum DataActionType {

    // <--[language]
    // @name Data Actions
    // @group Useful Lists
    // @description
    // Several commands function as a way to modify data values,
    // including <@link command flag>, <@link command yaml>, and <@link command define>.
    // These commands each allow for a set of generic data change operations.
    //
    // These operations can be used with a syntax like "<key>:<action>:<value>"
    // For example "mykey:+:5" will add 5 to the value at 'mykey'.
    //
    // The following actions are available:
    //
    // Actions that take no input value:
    // Increment: '++': raises the value numerically up by 1. Example: - define x:++
    // Decrement: '--': lowers the value numerically down by 1. Example: - define x:--
    // Clear: '!': removes the value entirely. Example: - define x:!
    //
    // Actions that take an input value:
    // Add: '+': adds the input value to the value at the key. Example: - define x:+:5
    // Subtract: '-': subtracts the input value from the value at the key. Example: - define x:-:5
    // Multiply: '*': multiplies the value at the key by the input value. Example: - define x:*:5
    // Divide: '/': divides the value at the key by the input value. Example: - define x:/:5
    // List insert: '->': adds the input value as a single new entry in the list (see also 'List split'). Example: - define x:->:new_value
    // List remove: '<-': removes the input value from the list. Example: - define x:<-:old_value
    // List split: '|': splits the input list and adds each value into an existing list at the key. Example: - define x:|:a|b|c
    // Split to new: '!|': similar to list split, but removes any existing value at the key first. Example: - define x:!|:a|b|c
    //
    // Special cases:
    // In some commands, specifying no action or input value will automatically set the key's value to 'true'.
    // Setting a '<key>:<value>' without an action will set the key to the exact value. Be careful to not input a list like this, use 'split to new' instead.
    //
    // Note that the <key> input may take an index input as well.
    // That is, for example: "mykey[3]:--" will decrement the third item in the list at 'mykey'.
    // This syntax may also be used to remove the entry at a specified index.
    //
    // -->

    /* Math */
    INCREMENT,
    DECREMENT,
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    /* Lists */
    INSERT,
    REMOVE,
    SPLIT,
    SPLIT_NEW,
    /* Value */
    SET,
    AUTO_SET,
    CLEAR
}

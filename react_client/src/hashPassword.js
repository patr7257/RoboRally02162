// Web Crypto API - SHA-256 -> hex
/*
Author(s): Lizette, Kajsa
*/
export async function sha256Hex(text) {
    const data = new TextEncoder().encode(text);
    const digest = await crypto.subtle.digest("SHA-256", data);
    return [...new Uint8Array(digest)]
        .map(b => b.toString(16).padStart(2, "0"))
        .join("");
}
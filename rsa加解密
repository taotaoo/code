package main

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha1"
	"crypto/x509"
	"encoding/hex"
	"encoding/pem"
	"errors"
	"log"
)

func main() {
	publicKey := "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvbGFofJxSJGKzG57LnaifOtsPLmBlpw8gZZ7oR/kOSDtx92oet2JyGVWYEnEippQUKL+1KYlWTM7Yc7b0Uk3iaMuN1hNZXUwfolMHQ+ImyAi5ygb3V+ZHAlxvWWIYw+w9KT9c+o9Lst0OnTLVS0LPPuqCIVr6LiYwKI438Swd6cS1tG/sHT0yP3GoquLEk/AuqtkYCyM/DohRKm4TBkOIMztONhdaplodaff/G8Cjt9f2dH7OCxtkrKyJeH1wWtOZlCbaCBLgm2t3ZxGJ4u5umumTzzEduwxgH1jVwsZXQmXQh5+dHERh1od/ccV5J8BJoOhxBND6xDV4Ts4HEmkjQIDAQAB"
	privateKey := "MIIEpAIBAAKCAQEAvbGFofJxSJGKzG57LnaifOtsPLmBlpw8gZZ7oR/kOSDtx92oet2JyGVWYEnEippQUKL+1KYlWTM7Yc7b0Uk3iaMuN1hNZXUwfolMHQ+ImyAi5ygb3V+ZHAlxvWWIYw+w9KT9c+o9Lst0OnTLVS0LPPuqCIVr6LiYwKI438Swd6cS1tG/sHT0yP3GoquLEk/AuqtkYCyM/DohRKm4TBkOIMztONhdaplodaff/G8Cjt9f2dH7OCxtkrKyJeH1wWtOZlCbaCBLgm2t3ZxGJ4u5umumTzzEduwxgH1jVwsZXQmXQh5+dHERh1od/ccV5J8BJoOhxBND6xDV4Ts4HEmkjQIDAQABAoIBAQCus/5FBop6sUBJwz7DrhM8RX4r9xV4Zm/7UWKPJFYGn2Me/fAbKEmrPFlu2MGgfTqW6WcU5efj3SFFlUQx3eK+aoE4C6VMWb/N6hklcpb3d4NtrSzslwzmG3SbYBVaVqs8xW+AAC1VGZ/z7nkN+ywsgAM7UdXuQYS9dSo4PGfh6XGAryDRClfMOPtbfBxfM9hIv6EfPPwYFt6XX2UhJp/TIoybEMsVmGt9EH9XCmw1GFMqRN8Jd6GjIlwgr4ntfaPrcr7Z44JUiGALypODJhRUSqG0s/DaWX/ncuuqdIZhscMif5aiLT/gLJfZPFikbHGxlyBlPPTYAaXlXeyB45qpAoGBANW+rBY61LBdYgd3OqL6UTh6Fgy+ViZGsLPDp7dEQ+uUAV2ZOHq245Lw3QlRzi/tWyC1MJtedzQj9C/qbj0iwIL0xxgrB6K2iZtJbxwFGYyr8hkSRFz370tQR/0AI92zxt6AepCmMMhJMKGcoN2glzsEsEjS/J+84gCALl/mnNFfAoGBAOMxpLw2zFCBfZEoTgA2h/iwLrGHQryicCnPkPyTKYrYljBY5e9qeyeMfqUhH9RJf2S4EYB/vMLDu4jPdb7142ku/MZKt9HYGmBhsXBHgjxqy/HC3x6Cbi0kagNqqKCFEHMYX4LQaipCBQtDXZqenwdhjYJeq8Rm9KjQqHgVk3WTAoGAPtuQ7gSlEayUrI9inhUxF4PHwj2jRiRZyLPMObgIpVnkQOtTUbtBF7BfwGLfWPbbarX+MmLIeXvtTr4JZ082AA0OE8xrtW0q8JDa7QmO5vCWBMt7cT+0jphwYzXMVmGNJVfxM2K8S9pCQ/S01hhpAQEy+meZxTwd1nnbqXY/Pc8CgYBv0uJzZUFhKqskRENIJY79X7JL3PiDIiy0155UQxbCaWJa/5aFJdLiH2vCWTBya46Rg2vR5I2DC5YFe59H030QK3ltHB+n4IbzA4Kzce7vT9177F+ng6k4/OBVOC0xfO0gyVFRcMWgcQhMh+bNkN9TYbemAHTo4Yfwg2s4V95RkwKBgQDUeeLf5iHO2jwLikupNVFLOO7FwK5JTkTIo50iGwB726p4xTCE60f3BLUnXWFpCH7JQagtE0+S8v8Hv+9V4bcoYJ9OGukkY3uim1ZnBZ1YKA6HxIbrT6UIRnB9GaMFomC/NCdLsjDkJPtaBUrA0RvQ1OluUakY6CF4FxXjO3ykIA=="
	txt := "hello rsa encrpt msg."
	encrypted, err := RsaEncrypt([]byte(txt), RsaPublicKey(publicKey))
	if err != nil {
		log.Printf("rsa encrypt failed, %v\n", err)
		return
	}
	log.Printf("encrypt msg : %v\n", hex.EncodeToString(encrypted))

	decrypted, err := RsaDecrypt(encrypted, RsaPrivateKey(privateKey))
	if err != nil {
		log.Printf("rsa decrypt failed, %v\n", err)
		return
	}
	log.Printf("decrypt msg : %v\n", string(decrypted))

}

// 拼接标准的publicKey
func RsaPublicKey(pub string) []byte {
	return []byte("-----BEGIN PUBLIC KEY-----\n" + pub + "\n-----END PUBLIC KEY-----\n")
}

// 拼接标准的rsaPrivateKey
func RsaPrivateKey(pri string) []byte {
	return []byte("-----BEGIN RSA PRIVATE KEY-----\n" + pri + "\n-----END RSA PRIVATE KEY-----\n")
}

func RsaEncrypt(origData []byte, pubKey []byte) ([]byte, error) {
	block, _ := pem.Decode(pubKey)
	if block == nil {
		return nil, errors.New("public key error")
	}
	pubInterface, err := x509.ParsePKIXPublicKey(block.Bytes)
	if err != nil {
		return nil, err
	}
	pub := pubInterface.(*rsa.PublicKey)
	// return rsa.EncryptPKCS1v15(rand.Reader, pub, origData)
	return rsa.EncryptOAEP(sha1.New(), rand.Reader, pub, origData, []byte(""))
}

func RsaDecrypt(ciphertext []byte, priKey []byte) ([]byte, error) {
	block, _ := pem.Decode(priKey)
	if block == nil {
		return nil, errors.New("private key error!")
	}
	priv, err := x509.ParsePKCS1PrivateKey(block.Bytes)
	if err != nil {
		return nil, err
	}
	// return rsa.DecryptPKCS1v15(rand.Reader, priv, ciphertext)
	return rsa.DecryptOAEP(sha1.New(), rand.Reader, priv, ciphertext, []byte(""))
}

$resetUrl = "http://localhost:8888/forgot-password"
$loginUrl = "http://localhost:8888/login"

# 1. Reset Password
echo "Resetting password..."
$resetResponse = Invoke-WebRequest -Uri $resetUrl -Method POST -Body @{username="user"; newPassword="newpass"} -MaximumRedirection 0 -ErrorAction SilentlyContinue

if ($resetResponse.StatusCode -eq 302) {
    echo "Reset POST successful (Redirected)."
} else {
    echo "Reset POST failed. Status: $($resetResponse.StatusCode)"
    exit
}

# 2. Login with NEW password
echo "Logging in with new password..."
$loginResponse = Invoke-WebRequest -Uri $loginUrl -Method POST -Body @{username="user"; password="newpass"} -MaximumRedirection 0 -ErrorAction SilentlyContinue

if ($loginResponse.StatusCode -eq 302) {
    echo "Login successful (Redirected to dashboard)."
    echo "Location: $($loginResponse.Headers['Location'])"
} else {
    echo "Login failed. Status: $($loginResponse.StatusCode)"
    # Check if we got the login page back with error
    if ($loginResponse.Content -like "*Incorrect password*") {
        echo "Error: Incorrect password."
    } elseif ($loginResponse.Content -like "*Username not found*") {
        echo "Error: Username not found."
    } else {
        echo "Unknown error."
    }
}
